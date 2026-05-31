using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Media.Animation;

namespace CrossDroid.Windows.Views
{
    public sealed partial class AboutView : Page
    {
        public AboutView()
        {
            this.InitializeComponent();
        }

        private void Page_Loaded(object sender, RoutedEventArgs e)
        {
            if (this.Resources.TryGetValue("EntryStoryboard", out object entryObj) && entryObj is Storyboard entryStoryboard)
            {
                entryStoryboard.Begin();
            }

            if (this.Resources.TryGetValue("InfiniteAnimations", out object infObj) && infObj is Storyboard infStoryboard)
            {
                infStoryboard.Begin();
            }

            if (this.Resources.TryGetValue("PulseAnimation", out object pulseObj) && pulseObj is Storyboard pulseStoryboard)
            {
                pulseStoryboard.Begin();
            }
        }

        private void BackButton_Click(object sender, RoutedEventArgs e)
        {
            App.MainWindowInstance.NavigateToPage("Settings");
        }
    }
}
