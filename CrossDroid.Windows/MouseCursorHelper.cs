using Microsoft.UI.Input;
using Microsoft.UI.Xaml;
using System.Reflection;

namespace CrossDroid.Windows
{
    /// <summary>
    /// Attached property helper that changes the mouse cursor to a Hand pointer
    /// on any UIElement when <c>HandCursor="True"</c> is set in XAML.
    ///
    /// Because <see cref="UIElement.ProtectedCursor"/> is a protected property in
    /// WinUI 3, we access it via Reflection – but only from PointerEntered/Exited
    /// event handlers, not during XAML parse time, to avoid crashing the XAML runtime.
    /// </summary>
    public static class MouseCursorHelper
    {
        // ------------------------------------------------------------------
        //  Attached property: HandCursor (bool)
        // ------------------------------------------------------------------
        public static readonly DependencyProperty HandCursorProperty =
            DependencyProperty.RegisterAttached(
                "HandCursor",
                typeof(bool),
                typeof(MouseCursorHelper),
                new PropertyMetadata(false, OnHandCursorChanged));

        public static bool GetHandCursor(DependencyObject obj)
            => (bool)obj.GetValue(HandCursorProperty);

        public static void SetHandCursor(DependencyObject obj, bool value)
            => obj.SetValue(HandCursorProperty, value);

        // ------------------------------------------------------------------
        //  Cached PropertyInfo – resolved once, reused for every element
        // ------------------------------------------------------------------
        private static readonly PropertyInfo? s_protectedCursorProp =
            typeof(UIElement).GetProperty(
                "ProtectedCursor",
                BindingFlags.NonPublic | BindingFlags.Instance);

        private static readonly InputCursor s_handCursor =
            InputSystemCursor.Create(InputSystemCursorShape.Hand);

        // FIX: A dedicated arrow cursor is used to reset the cursor on PointerExited.
        // Passing null to the native ProtectedCursor setter invokes the COM setter with
        // a null pointer, which raises an AccessViolationException — a corrupted-state
        // exception that bypasses the managed try/catch block entirely, crashing the app.
        private static readonly InputCursor s_arrowCursor =
            InputSystemCursor.Create(InputSystemCursorShape.Arrow);

        // ------------------------------------------------------------------
        //  Property changed callback – wire up events, NEVER set cursor here
        // ------------------------------------------------------------------
        private static void OnHandCursorChanged(
            DependencyObject d,
            DependencyPropertyChangedEventArgs e)
        {
            if (d is not UIElement element) return;

            bool enable = (bool)e.NewValue;

            // Detach old handlers first (idempotent)
            element.PointerEntered -= Element_PointerEntered;
            element.PointerExited  -= Element_PointerExited;

            if (enable)
            {
                element.PointerEntered += Element_PointerEntered;
                element.PointerExited  += Element_PointerExited;
            }
        }

        // ------------------------------------------------------------------
        //  Pointer event handlers – safe to call reflection from here
        // ------------------------------------------------------------------
        private static void Element_PointerEntered(object sender, Microsoft.UI.Xaml.Input.PointerRoutedEventArgs e)
        {
            if (sender is UIElement element)
                SetCursor(element, s_handCursor);
        }

        private static void Element_PointerExited(object sender, Microsoft.UI.Xaml.Input.PointerRoutedEventArgs e)
        {
            // FIX: PointerExited has a bubbling routing strategy in WinUI 3.
            // When the mouse moves between child elements inside a Button (e.g.
            // StackPanel → FontIcon → TextBlock), each child fires its own
            // PointerExited which bubbles up to the Button — even though the mouse
            // never actually left the button's visible bounds. Without this guard,
            // SetCursor is called with s_arrowCursor (formerly null) on every
            // inter-child transition, both disrupting the hand cursor mid-hover and
            // previously crashing the app via the null/AccessViolationException path.
            //
            // e.OriginalSource is the element the pointer physically left.
            // We only reset the cursor when the pointer truly exits THIS element.
            if (sender is UIElement element && ReferenceEquals(e.OriginalSource, sender))
                SetCursor(element, s_arrowCursor);
        }

        private static void SetCursor(UIElement element, InputCursor? cursor)
        {
            try
            {
                s_protectedCursorProp?.SetValue(element, cursor);
            }
            catch (System.Exception ex)
            {
                System.Diagnostics.Debug.WriteLine(
                    $"[MouseCursorHelper] SetCursor failed: {ex.Message}");
            }
        }
    }
}