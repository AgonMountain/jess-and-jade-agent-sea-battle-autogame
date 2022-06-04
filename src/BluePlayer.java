import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.AMSService;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.JADEAgentManagement.JADEManagementOntology;
import jade.domain.JADEAgentManagement.ShutdownPlatform;
import jade.lang.acl.ACLMessage;
import java.util.concurrent.TimeUnit;
import java.awt.Point;


public class BluePlayer extends Agent {

    public int[][] myGameField;
    public int[][] enemyGameField;
    public final int WIN_NUMBER = 20;

    public void setup() {
        System.out.println("Hi! I'm " + getAID().getLocalName() + " .");

        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    String senderAgentName = msg.getSender().getLocalName();
                    if (senderAgentName.equals("RedPlayer")) {
                        try {
                            TimeUnit.MILLISECONDS.sleep(250);
                        } catch (InterruptedException e) { }
                        getMessage(msg.getContent());
                    }
                }
                block();
            }
        });

        this.myGameField = this.generateFillGameField();
        this.enemyGameField = this.generateEmptyGameField(10, 10);

        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) { }

        System.out.println("Start game: Sea battle");
        System.out.println("Player:\t\t\tCoordinate:\t\t\tResult:");
        sendMessage("start");
    }

    // "command,coordinate_y,coordinate_x,result"
    public void getMessage(String message) {

        String[] split = message.split(",");
        String command = split[0];

        if (command.equals("end")) {
            printGameField();
            doDelete();
        }
        else if (command.equals("start")) {
            this.myGameField = this.generateFillGameField();
            this.enemyGameField = this.generateEmptyGameField(10, 10);
            shoot(getCoordinateForShoot());
        }
        else if (command.equals("shoot")) {
            Point coordinate = new Point(Integer.parseInt(split[2]), Integer.parseInt(split[1]));
            switch (this.myGameField[(int)coordinate.getY()][(int)coordinate.getX()]) {
                case 1:
                    printGameStepInfo("RedPlayer", coordinate, "hit");
                    sendMessage(generateMessage("result", coordinate, 1));
                    break;
                case 0:
                    printGameStepInfo("RedPlayer", coordinate, "away");
                    sendMessage(generateMessage("result", coordinate, -1));
                    break;
            }
        }
        else if (command.equals("result")) {
            Point coordinate = new Point(Integer.parseInt(split[2]), Integer.parseInt(split[1]));
            int result = Integer.parseInt(split[3]);
            this.enemyGameField[(int)coordinate.getY()][(int)coordinate.getX()] = result;
            if (isWin()) {
                System.out.println("Game Over! " + getAID().getLocalName() + " is Winner\n");
                printGameField();
                sendMessage("end");
            }
            else if (result == 1){
                shoot(getCoordinateForShoot());
            }
            else {
                sendMessage("skip"); // shot away -> turn player
            }
        }
        else {
            shoot(getCoordinateForShoot());
        }
    }

    /**
     * Проверить статус игры, победа
     * @return
     */
    public boolean isWin() {
        int winNumber = 0;

        for (int y = 0; y < this.enemyGameField.length; y++) {
            for (int x = 0; x < this.enemyGameField[y].length; x++) {
                if (this.enemyGameField[y][x] == 1) {
                    ++winNumber;
                }
            }
        }

        return (winNumber == this.WIN_NUMBER);
    }

    public int[][] generateFillGameField() {
        int[][] gameField =
                {   {0, 0, 0, 1, 0, 0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                        {0, 1, 0, 0, 0, 0, 0, 1, 0, 0},
                        {0, 0, 0, 1, 0, 0, 0, 1, 0, 0},
                        {0, 0, 0, 0, 0, 0, 0, 1, 0, 0},
                        {0, 0, 0, 0, 0, 1, 0, 1, 0, 0},
                        {0, 0, 0, 0, 0, 1, 0, 0, 0, 0},
                        {0, 0, 1, 1, 0, 0, 0, 1, 1, 1},
                        {0, 0, 0, 0, 0, 1, 0, 0, 0, 0},
                        {1, 0, 0, 0, 0, 1, 0, 1, 1, 1}  };

        return gameField;
    }

    public int[][] generateEmptyGameField(int height, int width) {
        int[][] emptyGameField = new int[height][width];

        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                emptyGameField[j][i] = 0;
            }
        }

        return emptyGameField;
    }

    /**
     * Выстрелить
     * @param coordinate координаты для выстрела
     */
    public void shoot(Point coordinate) {
        sendMessage(generateMessage("shoot", coordinate, 0));
    }

    /**
     * Получить координаты для выстрела
     * @return
     */
    public Point getCoordinateForShoot() {

        for (int y = 0; y < this.enemyGameField.length; y++) {
            for (int x = 0; x < this.enemyGameField[y].length; x++) {
                if (this.enemyGameField[y][x] == 0) {
                    return new Point(x, y);
                }
            }
        }
        return new Point(-1, -1);
    }

    /**
     * Сгенерировать сообщение для отправки
     * @param command команда
     * @param coordinate координаты
     * @param result результат выполнения команды
     * @return сообщение
     */
    public String generateMessage(String command, Point coordinate, int result) {
        return command + "," +
                Integer.toString((int)coordinate.getY()) + "," +
                Integer.toString((int)coordinate.getX()) + "," +
                Integer.toString(result);
    }

    /**
     * Напечатать сообщение о новом шаге игры
     * @param playerName имя игрока
     * @param coordinate коодинаты выстрела
     * @param messageInfo результат выстрела
     */
    public void printGameStepInfo(String playerName, Point coordinate, String messageInfo) {
        System.out.println(playerName + "\t\t\t" +
                "(" + Integer.toString((int)coordinate.getY() + 1) + "," + Integer.toString((int)coordinate.getX() + 1) + ")\t\t\t" +
                messageInfo);
    }

    public void printGameField() {
        System.out.println("\n" + getAID().getLocalName() + " game field:\n");
        for (int y = 0; y < this.enemyGameField.length; y++) {
            String line = "";
            for (int x = 0; x < this.enemyGameField[y].length; x++) {
                if (this.enemyGameField[y][x] == 1) {
                    line += " #";
                }
                else if (this.enemyGameField[y][x] == -1) {
                    line += " .";
                }
                else {
                    line += " ?";
                }
            }
            System.out.println(line);
        }
    }

    public void sendMessage(String message) {
        AMSAgentDescription[] agents = null;
        try {
            SearchConstraints c = new SearchConstraints();
            c.setMaxResults(Long.valueOf(-1));
            agents = AMSService.search(this, new AMSAgentDescription(), c);
        } catch (Exception e) {
            System.out.println("Problem searching AMS: " + e);
            e.printStackTrace();
        }

        for (AMSAgentDescription agent : agents) {
            AID agentID = agent.getName();
            if (agentID.getLocalName().equals("RedPlayer")) {
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.addReceiver(agentID);
                msg.setLanguage("English");
                msg.setContent(message);
                send(msg);
            }
        }
    }
}
