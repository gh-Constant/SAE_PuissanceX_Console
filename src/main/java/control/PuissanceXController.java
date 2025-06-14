package control;

import boardifier.control.ActionFactory;
import boardifier.control.ActionPlayer;
import boardifier.control.Controller;
import boardifier.control.Decider;
import boardifier.model.GameException;
import boardifier.model.Model;
import boardifier.model.Player;
import boardifier.model.action.ActionList;
import boardifier.view.View;
import model.PuissanceXBoard;
import model.PuissanceXDisk;
import model.PuissanceXStageModel;

import java.util.Scanner;

/**
 * Controller for the PuissanceX game.
 * Handles game flow, user input, and interactions between main.model and main.view.
 */
public class PuissanceXController extends Controller {

    private Scanner scanner;
    private Decider aiDecider;
    private Decider secondAiDecider;
    private int endGameChoice = 0; // 0 = pas encore défini, 1 = rejouer, 2 = menu principal, 3 = quitter

    /**
     * Constructor for the PuissanceX controller.
     */
    public PuissanceXController(Model model, View view) {
        super(model, view);
        this.scanner = new Scanner(System.in);
        this.aiDecider = null;
        this.secondAiDecider = null;
    }
    
    public void setAIDecider(Decider decider) {
        this.aiDecider = decider;
    }

    public void setSecondAIDecider(Decider decider) {
        this.secondAiDecider = decider;
    }

    /**
     * Starts the game by initializing the main.model and main.view.
     */
    @Override
    public void startGame() {
        System.out.println("DEBUG: PuissanceXController.startGame() called.");
        try {
            super.startGame();
            System.out.println("DEBUG: super.startGame() completed successfully.");
        } catch (GameException e) {
            System.err.println("ERROR in startGame: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Main game loop that handles player turns and game state.
     */
    public void stageLoop() {
        update();
        
        while (!model.isEndStage()) {
            playTurn();
            if (!model.isEndStage()) {
                endOfTurn();
                update();
            }
        }

        update();

        // Game over
        displayGameResult();
    }

    public void playTurn() {
        PuissanceXStageModel stageModel = (PuissanceXStageModel) model.getGameStage();
        PuissanceXBoard board = stageModel.getBoard();
        int currentPlayer = model.getIdPlayer();
        Player player = model.getPlayers().get(currentPlayer);
        
        System.out.println("Player " + (currentPlayer + 1) + " (" + model.getCurrentPlayerName() + "), your turn.");
        
        // Check if current player is AI
        if (player.getType() == Player.COMPUTER) {
            System.out.println("AI is thinking...");
            
            // Use the appropriate AI decider based on the current player
            Decider currentDecider;
            if (currentPlayer == 0) {
                currentDecider = aiDecider;
            } else {
                currentDecider = secondAiDecider;
            }
            
            if (currentDecider != null) {
                ActionList actions = currentDecider.decide();
                if (actions != null) {
                    ActionPlayer actionPlayer = new ActionPlayer(model, this, actions);
                    actionPlayer.start();
                    checkGameEndConditions(board);
                } else {
                    System.out.println("AI couldn't make a valid move!");
                }
            } else {
                System.out.println("Error: AI decider not set!");
            }
        } else {
            // Human player's turn - existing code for human input
            handleHumanPlayerTurn(board, currentPlayer);
        }
    }
    
    private void handleHumanPlayerTurn(PuissanceXBoard board, int currentPlayer) {
        boolean validMove = false;
        
        while (!validMove) {
            System.out.println("Enter column number (1-" + board.getNbCols() + "): ");
            try {
                String input = scanner.nextLine();
                int userCol = Integer.parseInt(input);
                
                // Convert from 1-based (user-friendly) to 0-based (internal)
                int col = userCol - 1;
                
                // Check if the column is valid
                if (col < 0 || col >= board.getNbCols()) {
                    System.out.println("Invalid column number. Please enter a number between 1 and " + board.getNbCols() + ".");
                    continue;
                }
                
                // Check if the column is full
                if (board.isColumnFull(col)) {
                    System.out.println("Column " + userCol + " is full. Try another one.");
                    continue;
                }
                
                // Find the first empty row in the column
                int row = board.getFirstEmptyRow(col);
                
                // Create a new disk
                PuissanceXDisk disk = new PuissanceXDisk(currentPlayer, (PuissanceXStageModel)model.getGameStage());
                
                // Create an action to place the disk
                ActionList actions = ActionFactory.generatePutInContainer(this, model, disk, "board", row, col);
                actions.setDoEndOfTurn(true);
                
                // Play the action
                ActionPlayer actionPlayer = new ActionPlayer(model, this, actions);
                actionPlayer.start();
                
                // Check for win or draw
                checkGameEndConditions(board);
                
                validMove = true;
                
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }
    
    private void checkGameEndConditions(PuissanceXBoard board) {
        PuissanceXStageModel stageModel = (PuissanceXStageModel) model.getGameStage();
        int currentPlayer = model.getIdPlayer();
        
        // Check for win - this assumes the last move was at the last filled position
        for (int col = 0; col < board.getNbCols(); col++) {
            int row = board.getFirstEmptyRow(col);
            if (row < board.getNbRows() - 1) {  // If there's a piece below the empty spot
                if (stageModel.checkWin(row + 1, col, currentPlayer)) {
                    model.setIdWinner(currentPlayer);
                    model.stopStage();
                    return;
                }
            }
        }
        
        // Check for draw
        if (stageModel.isBoardFull()) {
            model.stopStage();
        }
    }
    
    @Override
    public void endOfTurn() {
        model.setNextPlayer();
        
        // Update the player text
        PuissanceXStageModel stageModel = (PuissanceXStageModel) model.getGameStage();
        stageModel.getPlayerText().setText("Current player: " + model.getCurrentPlayerName());
    }
    
    private void displayGameResult() {
        int winnerId = model.getIdWinner();
        
        if (winnerId != -1) {
            System.out.println("Player " + (winnerId + 1) + " (" + model.getPlayers().get(winnerId).getName() + ") wins!");
        } else {
            System.out.println("Game ended in a draw!");
        }
        
        // Afficher le menu de fin de partie
        showEndGameOptions();
    }
    
    private void showEndGameOptions() {
        System.out.println("\n╔════════════════════════════════════╗");
        System.out.println("║            PARTIE TERMINÉE         ║");
        System.out.println("╠════════════════════════════════════╣");
        System.out.println("║ 1. Rejouer                         ║");
        System.out.println("║ 2. Retour au menu principal        ║");
        System.out.println("║ 3. Quitter                         ║");
        System.out.println("╚════════════════════════════════════╝");
        
        boolean validChoice = false;
        while (!validChoice) {
            System.out.print("Entrez votre choix (1-3): ");
            try {
                String input = scanner.nextLine().trim();
                int choice = Integer.parseInt(input);
                
                if (choice >= 1 && choice <= 3) {
                    endGameChoice = choice;
                    validChoice = true;
                } else {
                    System.out.println("Veuillez entrer un nombre entre 1 et 3.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Veuillez entrer un nombre valide.");
            }
        }
    }
    
    /**
     * Retourne le choix de fin de partie du joueur.
     * @return 1 = rejouer, 2 = menu principal, 3 = quitter
     */
    public int getEndGameChoice() {
        return endGameChoice;
    }
    
    /**
     * Réinitialise le choix de fin de partie.
     */
    public void resetEndGameChoice() {
        endGameChoice = 0;
    }
}