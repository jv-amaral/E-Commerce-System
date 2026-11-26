import controller.UserController;
import view.MarketPlaceSwing;
import view.MarketPlaceView;

/** Ponto de entrada que conecta Controller e View na arquitetura MVC. */
public class MarketPlace {
    public static void main(String[] args) {
        UserController controller = new UserController();
        if (args.length > 0 && args[0].equalsIgnoreCase("--gui")) {
            MarketPlaceSwing.iniciarInterface(controller);
        } else {
            new MarketPlaceView(controller).painelLogIn();
        }
    }
}
