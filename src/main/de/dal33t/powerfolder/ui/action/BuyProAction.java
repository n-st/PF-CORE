package de.dal33t.powerfolder.ui.action;

import java.awt.event.ActionEvent;
import java.io.IOException;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.BrowserLauncher;

public class BuyProAction extends BaseAction {

    public BuyProAction(Controller controller) {
        super("buypro", controller);
    }

    public void actionPerformed(ActionEvent e) {
        try {
            BrowserLauncher
                .openURL("http://www.powerfolder.com/node/pro_edition");
        } catch (IOException ex) {
            log().error(ex);
        }
    }

}
