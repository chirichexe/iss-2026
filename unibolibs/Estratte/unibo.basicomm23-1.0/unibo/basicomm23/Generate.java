package unibo.basicomm23;

public class Generate {
    /**
     * Stampa l'elenco dei package presenti nell'applicazione unibo.basicomm23.
     */
    public static void main(String[] args) {
        String[] packages = {
            "unibo.basicomm23.bth",
            "unibo.basicomm23.coap",
            "unibo.basicomm23.enablers",
            "unibo.basicomm23.eureka",
            "unibo.basicomm23.examples",
            "unibo.basicomm23.http",
            "unibo.basicomm23.interfaces",
            "unibo.basicomm23.mqtt",
            "unibo.basicomm23.msg",
            "unibo.basicomm23.serial",
            "unibo.basicomm23.tcp",
            "unibo.basicomm23.udp",
            "unibo.basicomm23.utils",
            "unibo.basicomm23.ws"
        };
        System.out.println("Elenco dei package disponibili in unibo.basicomm23:");
        for (String pkg : packages) {
            System.out.println("- " + pkg);
        }
    }
}
