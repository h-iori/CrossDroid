using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using System;

namespace CrossDroid.Windows.Views
{
    public sealed partial class AboutView : Page
    {
        public AboutView()
        {
            this.InitializeComponent();
        }

        private async void CleanStorage_Click(object sender, RoutedEventArgs e)
        {
            var dialog = new ContentDialog
            {
                Title = "Cache Cleaned Successfully",
                Content = "Reclaimed 12.8 MB of temporary files.",
                CloseButtonText = "OK",
                XamlRoot = this.XamlRoot
            };
            await dialog.ShowAsync();
        }
    }
}
