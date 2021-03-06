import java.lang.InterruptedException;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.AMSService;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.lang.acl.ACLMessage;
import java.awt.Point;


public class Player2 extends Agent {

    String playerName = "Player2";
    String enemyPlayerName = "Player1";
    int millisecondsTimeOut = 250;

    public String[][] myGameField;
    public String[][] enemyGameField;
    public final int WIN_NUMBER = 20;

    public void setup() {
        System.out.println("Hi! I'm " + getAID().getLocalName() + " .");

        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    String senderAgentName = msg.getSender().getLocalName();
                    if (senderAgentName.equals("Player1")) {
                        try {
                            TimeUnit.MILLISECONDS.sleep(millisecondsTimeOut);
                        } catch (InterruptedException e) { }
                        getMessage(msg.getContent());
                    }
                }
                block();
            }
        });
    }

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
                case "#":
                    printGameStepInfo(enemyPlayerName, coordinate, "hit");
                    sendMessage("result," + toString(coordinate) + "," + "#");
                    break;
                case "":
                    printGameStepInfo(enemyPlayerName, coordinate, "away");
                    sendMessage("result," + toString(coordinate) + "," + ".");
                    break;
            }
        }
        else if (command.equals("ship_is_destroyed")) {
            // ship_is_destroyed,1,1,1,2,1,3,1,4

            for (int i = 0; i < (split.length - 1) / 2; i++) {
                Point coordinate = new Point(Integer.parseInt(split[2+i]), Integer.parseInt(split[1+i]));
                updateGameField(coordinate);
            }

        }
        else if (command.equals("result")) {
            Point coordinate = new Point(Integer.parseInt(split[2]), Integer.parseInt(split[1]));
            String result = split[3];

            this.enemyGameField[(int)coordinate.getY()][(int)coordinate.getX()] = result;
            if (isWin()) {
                System.out.println("\n====================================================");
                System.out.println("\nGame Over! " + playerName + " is Winner");
                printGameField();
                sendMessage("end");
            }
            else if (result.equals("#")){
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

    public boolean isWin() {
        int winNumber = 0;

        for (int y = 0; y < this.enemyGameField.length; y++) {
            for (int x = 0; x < this.enemyGameField[y].length; x++) {
                if (this.enemyGameField[y][x].equals("#")) {
                    ++winNumber;
                }
            }
        }

        return (winNumber == this.WIN_NUMBER);
    }

    public String[][] generateFillGameField() {
        String[][] gameField =
                {       {"#", "", "", "", "", "#", "", "", "", "#"},
                        {"", "", "", "", "", "", "", "", "", "#"},
                        {"", "", "", "", "", "", "", "", "", ""},
                        {"", "", "#", "#", "", "#", "", "", "", ""},
                        {"", "", "", "", "", "#", "", "#", "#", "#"},
                        {"", "", "", "", "", "#", "", "", "", ""},
                        {"", "", "", "", "", "#", "", "", "", ""},
                        {"", "", "#", "", "", "", "", "#", "#", "#"},
                        {"", "", "", "", "#", "", "", "", "", ""},
                        {"", "", "", "", "#", "", "", "#", "", ""}
                };

        return gameField;
    }

    public String[][] generateEmptyGameField(int height, int width) {
        String[][] emptyGameField = new String[height][width];

        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                emptyGameField[j][i] = "";
            }
        }

        return emptyGameField;
    }

    public void shoot(Point coordinate) {
        sendMessage("shoot," + toString(coordinate));
    }

    public Point getCoordinateForShoot() {

        Point coordinate = getCoordinateByGameFieldContext();

        if ((int)coordinate.getX() == -1) {
            ArrayList<Point> coordinateList = new ArrayList<>();

            for (int y = 0; y < this.enemyGameField.length; y++) {
                for (int x = 0; x < this.enemyGameField[y].length; x++) {
                    if (this.enemyGameField[y][x].equals("")) {
                        coordinateList.add(new Point(x, y));
                    }
                }
            }

            if (!coordinateList.isEmpty()) {
                int i = ThreadLocalRandom.current().nextInt(0, coordinateList.size() - 1);
                coordinate = coordinateList.get(i);
            }
        }

        return coordinate;
    }

    public Point getCoordinateByGameFieldContext() {

        ArrayList<Point> coordinateList = new ArrayList<>();

        for (int y = 0; y < this.enemyGameField.length; y++) {
            for (int x = 0; x < this.enemyGameField[y].length; x++) {
                if (this.enemyGameField[y][x].equals("#")) {
                    coordinateList.add(new Point(x, y));
                }
            }
        }

        for (int i = 0; i < coordinateList.size(); i++) {
            int x = (int)coordinateList.get(i).getX();
            int y = (int)coordinateList.get(i).getY();

            if (x != -1) {
                if ((y - 1) >= 0) {
                    if (this.enemyGameField[y - 1][x].equals("")) {
                        return new Point(x, y - 1);
                    }
                }
                if ((y + 1) < this.enemyGameField.length) {
                    if (this.enemyGameField[y + 1][x].equals("")) {
                        return new Point(x, y + 1);
                    }
                }
                if ((x - 1) >= 0) {
                    if (this.enemyGameField[y][x - 1].equals("")) {
                        return new Point(x - 1, y);
                    }
                }
                if ((x + 1) < this.enemyGameField[y].length) {
                    if (this.enemyGameField[y][x + 1].equals("")) {
                        return new Point(x + 1, y);
                    }
                }
            }
        }

        return new Point(-1, -1);
    }

    public void updateGameField(Point coordinate) {

        int x = (int)coordinate.getX();
        int y = (int)coordinate.getY();

        if ((y - 1) >= 0) {
            if (this.enemyGameField[y - 1][x].equals("")) {
                this.enemyGameField[y - 1][x] = "~";
            }
        }
        if ((y + 1) < this.enemyGameField.length) {
            if (this.enemyGameField[y + 1][x].equals("")) {
                this.enemyGameField[y + 1][x] = "~";
            }
        }
        if ((x - 1) >= 0) {
            if (this.enemyGameField[y][x - 1].equals("")) {
                this.enemyGameField[y][x - 1] = "~";
            }
        }
        if ((x + 1) < this.enemyGameField[y].length) {
            if (this.enemyGameField[y][x + 1].equals("")) {
                this.enemyGameField[y][x + 1] = "~";
            }
        }

    }

    public void printGameStepInfo(String playerName, Point coordinate, String messageInfo) {
        System.out.println(playerName + "\t\t\t" +
                "(" + Integer.toString((int)coordinate.getY() + 1) + "," + Integer.toString((int)coordinate.getX() + 1) + ")\t\t\t\t" +
                messageInfo);
    }

    public void printGameField() {
        System.out.println("\nEnemy (" + enemyPlayerName + ") game field:\n");
        for (int y = 0; y < this.enemyGameField.length; y++) {
            String line = "";
            for (int x = 0; x < this.enemyGameField[y].length; x++) {
                if (this.enemyGameField[y][x].equals("")) {
                    line += "?" + " ";
                }
                else {
                    line += this.enemyGameField[y][x] + " ";
                }
            }
            System.out.println(line);
        }
    }

    public String toString(Point coordinate){
        return (Integer.toString((int)coordinate.getY()) + "," + Integer.toString((int)coordinate.getX()));
    }

    public void sendMessage(String text) {
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
            if (agentID.getLocalName().equals("Player1")) {
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.addReceiver(agentID);
                msg.setLanguage("English");
                msg.setContent(text);
                send(msg);
            }
        }
    }
}
