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
            if (sender is UIElement element)
                SetCursor(element, null);
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
