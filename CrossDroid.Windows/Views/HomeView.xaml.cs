using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Media.Animation;
using System;

namespace CrossDroid.Windows.Views
{
    public sealed partial class HomeView : Page
    {

        private readonly Random _random = new();

        public HomeView()
        {
            this.InitializeComponent();
            this.Loaded += HomeView_Loaded;
        }

        private void HomeView_Loaded(object sender, RoutedEventArgs e)
        {
            // Initialize with a random PIN
            GenerateNewPin();


        }


        private void VisibilityToggle_Toggled(object sender, RoutedEventArgs e)
        {
            if (VisibilityToggle == null || DiscoveryPanel == null || HiddenPanel == null || VisibilityStatusText == null) 
                return;

            if (VisibilityToggle.IsOn)
            {
                DiscoveryPanel.Visibility = Visibility.Visible;
                HiddenPanel.Visibility = Visibility.Collapsed;
                VisibilityStatusText.Text = "Visible to nearby devices";
                VisibilityStatusText.Foreground = (Microsoft.UI.Xaml.Media.Brush)Application.Current.Resources["ColorSuccess"];


            }
            else
            {
                DiscoveryPanel.Visibility = Visibility.Collapsed;
                HiddenPanel.Visibility = Visibility.Visible;
                VisibilityStatusText.Text = "Hidden from nearby devices";
                VisibilityStatusText.Foreground = (Microsoft.UI.Xaml.Media.Brush)Application.Current.Resources["ColorError"];


            }
        }

        private void RefreshButton_Click(object sender, RoutedEventArgs e)
        {
            GenerateNewPin();
        }

        private void GenerateNewPin()
        {
            int pin = _random.Next(1000, 10000);
            PinTextBlock.Text = pin.ToString();
        }
    }
}
