import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.AMSService;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.lang.acl.ACLMessage;

import java.awt.*;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;

public class Player1 extends Agent {

    String playerName = "Player1";
    String enemyPlayerName = "Player2";
    int millisecondsTimeOut = 250;

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
                    if (senderAgentName.equals("Player2")) {
                        try {
                            TimeUnit.MILLISECONDS.sleep(millisecondsTimeOut);
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
        System.out.println("\nPlayer:\t\t\tCoordinate:\t\t\tResult:");
        sendMessage("start");
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
                case 1:
                    printGameStepInfo(enemyPlayerName, coordinate, "hit");
                    sendMessage(generateMessage("result", coordinate, 1));
                    break;
                case 0:
                    printGameStepInfo(enemyPlayerName, coordinate, "away");
                    sendMessage(generateMessage("result", coordinate, -1));
                    break;
            }
        }
        else if (command.equals("result")) {
            Point coordinate = new Point(Integer.parseInt(split[2]), Integer.parseInt(split[1]));
            int result = Integer.parseInt(split[3]);
            this.enemyGameField[(int)coordinate.getY()][(int)coordinate.getX()] = result;
            if (isWin()) {
                System.out.println("\n====================================================");
                System.out.println("\nGame Over! " + playerName + " is Winner");
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

    public void shoot(Point coordinate) {
        sendMessage(generateMessage("shoot", coordinate, 0));
    }

    public Point getCoordinateForShoot() {

        Point coordinate = getCoordinateByGameFieldContext();

        if ((int)coordinate.getX() == -1) {
            ArrayList<Point> coordinateList = new ArrayList<>();

            for (int y = 0; y < this.enemyGameField.length; y++) {
                for (int x = 0; x < this.enemyGameField[y].length; x++) {
                    if (this.enemyGameField[y][x] == 0) {
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
                if (this.enemyGameField[y][x] == 1) {
                    coordinateList.add(new Point(x, y));
                }
            }
        }

        for (int i = 0; i < coordinateList.size(); i++) {
            int x = (int)coordinateList.get(i).getX();
            int y = (int)coordinateList.get(i).getY();

            if (x != -1) {
                if ((y - 1) >= 0) {
                    if (this.enemyGameField[y - 1][x] == 0) {
                        return new Point(x, y - 1);
                    }
                }
                if ((y + 1) < this.enemyGameField.length) {
                    if (this.enemyGameField[y + 1][x] == 0) {
                        return new Point(x, y + 1);
                    }
                }
                if ((x - 1) >= 0) {
                    if (this.enemyGameField[y][x - 1] == 0) {
                        return new Point(x - 1, y);
                    }
                }
                if ((x + 1) < this.enemyGameField[y].length) {
                    if (this.enemyGameField[y][x + 1] == 0) {
                        return new Point(x + 1, y);
                    }
                }
            }
        }

        return new Point(-1, -1);
    }

    public String generateMessage(String command, Point coordinate, int result) {
        return command + "," +
                Integer.toString((int)coordinate.getY()) + "," +
                Integer.toString((int)coordinate.getX()) + "," +
                Integer.toString(result);
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
            if (agentID.getLocalName().equals("Player2")) {
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.addReceiver(agentID);
                msg.setLanguage("English");
                msg.setContent(message);
                send(msg);
            }
        }
    }
}
