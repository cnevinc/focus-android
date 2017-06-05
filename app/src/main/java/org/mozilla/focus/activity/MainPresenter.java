package org.mozilla.focus.activity;


public class MainPresenter {

    private static MainPresenter INSTANCE;
    private ViewContract mainViewContract;
    private String pendingUrl;

    public void updateUrl(String dataString) {
        this.pendingUrl = dataString;
    }


    interface ViewContract {
        boolean shouldShowFirstrun();

        void onShowBrowserScreen(String pendingUrl);

        void showHomeScreen();

        void showFirstrun();
    }


    static MainPresenter getInstnace(ViewContract viewContract) {
        if (INSTANCE == null) {
            INSTANCE = new MainPresenter(viewContract);
        } else {
            INSTANCE.mainViewContract = viewContract;
        }
        return INSTANCE;
    }


    private MainPresenter(ViewContract viewContract) {
        this.mainViewContract = viewContract;
    }

    void showBrowserScreen() {
        if (pendingUrl != null && !mainViewContract.shouldShowFirstrun()) {
            // We have received an URL in onNewIntent(). Let's load it now.
            // Unless we're trying to show the firstrun screen, in which case we leave it pending until
            // firstrun is dismissed.
            mainViewContract.onShowBrowserScreen(pendingUrl);
            pendingUrl = null;
        }
    }

    void doFirstrunFinished() {
        if (pendingUrl != null) {
            // We have received an URL in onNewIntent(). Let's load it now.
            showBrowserScreen();
            pendingUrl = null;
        } else {
            mainViewContract.showHomeScreen();
        }
    }

    public void handleViewAction(boolean isViewAction, String url) {
        if (isViewAction) {

            if (mainViewContract.shouldShowFirstrun()) {
                pendingUrl = url;
                mainViewContract.showFirstrun();
            } else {
                showBrowserScreen();
            }
        } else {
            if (mainViewContract.shouldShowFirstrun()) {
                mainViewContract.showFirstrun();
            } else {
                mainViewContract.showHomeScreen();
            }
        }
    }
}
