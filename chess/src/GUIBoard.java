import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.nio.file.Paths;
import java.util.HashSet;


// https://www.youtube.com/watch?v=SNYFjgz4bU4
// png files from https://marcelk.net/chess/pieces/

/*
1) Class Constructor
2) Board Labelling Methods
3) Board Formatting Methods
4) Information Panel Method
5) Piece Movement Methods
6) Game Saving Class
7) Piece Movement Methods
8) Overridden Methods
*/

/**
 * The GUIBoard class contains all the methods and classes used to display the game of chess
 */

public class GUIBoard extends JFrame {

    // the pieces used for the game
    private final Pieces pieces;
    private final int dimension = BOARD.LAST_RANK.getRankVal();
    private final int firstRank = BOARD.FIRST_RANK.getRankVal();
    private final char firstFile = BOARD.FIRST_FILE.getFileVal();
    private final char lastFile = BOARD.LAST_FILE.getFileVal();
    private final char charFile = (char) (firstFile - 1);
    // the turn of the game being played
    private static COLOUR turn = COLOUR.W;

    // text pane containing the moves of a game
    private final JTextPane movePane = new JTextPane();
    // text pane containing the message at the end of the game (mate or draw)
    private final JTextPane matePane = new JTextPane();
    
    // panels to display captured pieces
    private final JPanel capturedWhitePiecesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 3));
    private final JPanel capturedBlackPiecesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 3));
    
    // panels for captured pieces with labels
    private final JPanel whitePanel = new JPanel(new BorderLayout());
    private final JPanel blackPanel = new JPanel(new BorderLayout());
    
    // size for captured piece images
    private static final int capturedPieceSize = 30;
    private static final int infoPanelWidth = 200;  // Fixed width for info panel

    // the board on which the game is played
    private final JButton[][] board = new JButton[dimension][dimension];
    // the button used to save a game
    private final JButton saveButton = new JButton("Save Game");

    // colours for the board
    private final Color brown = new Color(150, 75, 0); //brown #964B00
    private final Color pastel = new Color(255, 222, 173); //navajorwhite #FFDEAD
    private final Color intermediate = new Color(255, 255, 153);
    private final Color captureHighlight = new Color(255, 102, 102); // Light red for capture positions
    public static final Color infoColour = new Color(51,51,51);

    // size of a square in board
    private static final int tileSize = 88;

    // icon for squares without pieces
    private final BufferedImage invisible = new BufferedImage(80, 80, BufferedImage.TYPE_INT_ARGB);
    private final ImageIcon invisibleIcon = new ImageIcon(invisible);

    // used to determine number of clicks
    private int counter = 0;

    // number of turns used for game saving
    private int numberOfTurns = 0;

    // build up the game save file
    private static final StringBuilder str = new StringBuilder();

    // piece selected to move
    private Piece movingPiece;

    // ActionHandler used in GUIBoard construcotr
    ButtonHandle gameClick = new ButtonHandle();

    // size for captured piece images
    private static final int capturedPanelHeight = 80;  // Height of captured pieces panel

    // Custom FlowLayout that wraps components
    private static class WrapLayout extends FlowLayout {
        public WrapLayout(int align, int hgap, int vgap) {
            super(align, hgap, vgap);
        }

        @Override
        public Dimension minimumLayoutSize(Container target) {
            return layoutSize(target, true);
        }

        @Override
        public Dimension preferredLayoutSize(Container target) {
            return layoutSize(target, true);
        }

        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int targetWidth = target.getWidth();
                if (targetWidth == 0) {
                    targetWidth = Integer.MAX_VALUE;
                }

                int hgap = getHgap();
                int vgap = getVgap();
                Insets insets = target.getInsets();
                int maxWidth = targetWidth - (insets.left + insets.right);
                
                int x = 0;
                int y = insets.top;
                int rowHeight = 0;

                int nmembers = target.getComponentCount();
                for (int i = 0; i < nmembers; i++) {
                    Component m = target.getComponent(i);
                    if (m.isVisible()) {
                        Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                        if (x > 0 && x + d.width > maxWidth) {
                            x = 0;
                            y += rowHeight + vgap;
                            rowHeight = 0;
                        }
                        x += d.width + hgap;
                        rowHeight = Math.max(rowHeight, d.height);
                    }
                }
                y += rowHeight + insets.bottom;
                return new Dimension(maxWidth, y);
            }
        }
    }

    //________________________________________________Class Constructor________________________________________________

    /**
     * The GUIBoard construcotr creates the chess board
     * It assigns buttons to the boards 2D array, and adds their ActionListeners (gameClick)
     * Adds all the additional JPanels (i.e to show files, rows, save panels, etc...)
     * Sets up the JFrame
     * @param p the pieces HashMap used for the game
     */

    public GUIBoard(Pieces p) {
        setTitle("CHESS23");
        setBackground(Color.black);
        Container contents = getContentPane();
        contents.setLayout(new BorderLayout(5, 0));

        pieces = p;
        
        // Set up captured pieces panels
        capturedWhitePiecesPanel.setBackground(infoColour);
        capturedBlackPiecesPanel.setBackground(infoColour);
        whitePanel.setBackground(infoColour);
        blackPanel.setBackground(infoColour);
        
        // Create scroll panes for captured pieces
        JScrollPane whiteScroll = new JScrollPane(capturedWhitePiecesPanel);
        whiteScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        whiteScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        whiteScroll.setPreferredSize(new Dimension(infoPanelWidth - 20, 100));
        whiteScroll.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        
        JScrollPane blackScroll = new JScrollPane(capturedBlackPiecesPanel);
        blackScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        blackScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        blackScroll.setPreferredSize(new Dimension(infoPanelWidth - 20, 100));
        blackScroll.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        
        // Add labels for captured pieces
        JLabel whiteCapturedLabel = new JLabel("White Pieces Captured:");
        whiteCapturedLabel.setForeground(Color.WHITE);
        whiteCapturedLabel.setFont(new Font("Arial", Font.BOLD, 12));
        JPanel whiteLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        whiteLabelPanel.setBackground(infoColour);
        whiteLabelPanel.add(whiteCapturedLabel);
        
        JLabel blackCapturedLabel = new JLabel("Black Pieces Captured:");
        blackCapturedLabel.setForeground(Color.WHITE);
        blackCapturedLabel.setFont(new Font("Arial", Font.BOLD, 12));
        JPanel blackLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        blackLabelPanel.setBackground(infoColour);
        blackLabelPanel.add(blackCapturedLabel);

        // Set up the panels with labels and scroll panes
        whitePanel.add(whiteLabelPanel, BorderLayout.NORTH);
        whitePanel.add(whiteScroll, BorderLayout.CENTER);
        blackPanel.add(blackLabelPanel, BorderLayout.NORTH);
        blackPanel.add(blackScroll, BorderLayout.CENTER);

        JPanel boardPanel = new JPanel(new GridLayout(dimension, dimension));
        for (int rank = dimension; rank >= firstRank; rank--) {
            for (int file = 1; file <= dimension; file++) {
                char fileChar = (char) (charFile + file);
                Coordinate tileCoord = new Coordinate(fileChar, rank);
                board[rank - 1][file - 1] = setButton(tileCoord, p);
                board[rank - 1][file - 1].addActionListener(gameClick);
                boardPanel.add(board[rank - 1][file - 1]);
            }
        }

        JPanel fullBoardPanel = new JPanel(new BorderLayout());
        fullBoardPanel.add(createFileLabelsTop(),BorderLayout.NORTH);
        fullBoardPanel.add(createRankLabelsLeft(),BorderLayout.WEST);
        fullBoardPanel.add(boardPanel, BorderLayout.CENTER);
        fullBoardPanel.add(createRankLabelsRight(),BorderLayout.EAST);
        fullBoardPanel.add(createFileLabelsBottom(), BorderLayout.SOUTH);
        contents.add(fullBoardPanel, BorderLayout.WEST);
        contents.add(createInfoPanel(), BorderLayout.EAST);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    //________________________________________________Board Labelling Methods________________________________________________

    /**
     * Shows the ranks (1-8) of the board
     * @return a JPanel with JLabels containing the ranks of the chess board
     */

    public JPanel createRanks() {
        JPanel ranks = new JPanel(new GridLayout(dimension,0));
        ranks.setBackground(pastel);

        int rankPad = 4;

        for (int rank = dimension; rank >= firstRank; rank--) {
            JLabel rankLabel = new JLabel(String.valueOf(rank));
            rankLabel.setFont(new Font("TimesRoman", Font.BOLD, 23));
            rankLabel.setForeground(brown);
            rankLabel.setBorder(new EmptyBorder(0,rankPad,0,rankPad));
            ranks.add(rankLabel);
        }

        return ranks;
    }

    /**
     * Shows the files (a - h) of the board
     * @return a JPanel with JLabels containing the files of the chess board
     */

    public JPanel createFiles() {
        JPanel files = new JPanel(new GridLayout(0,dimension));
        files.setBackground(pastel);

        int filePad = 42;

        for (char file = firstFile; file <= lastFile; file++) {
            JLabel fileLabel = new JLabel(String.valueOf(file));

            fileLabel.setFont(new Font("TimesRoman", Font.BOLD, 23));
            fileLabel.setForeground(brown);
            fileLabel.setBorder(new EmptyBorder(0,filePad,0,filePad));
            files.add(fileLabel);
        }
        return files;
    }

    /**
     * Shows a brown border for the board
     * @return a JPanel that acts as a border for the baord
     */

    public JPanel createBorder() {
        JPanel border = new JPanel();
        border.setBackground(brown);

        return border;
    }

    /**
     * @return a JPanel, acting as a corner for the board
     */

    public Border createCorner() {
        int borderPad = 20;
        return new MatteBorder(0,borderPad,0,borderPad,pastel);
    }

    /**
     * Displays the ranks to the left of the board
     * @return a JPanel displaying the ranks and the border displayed to the left of the board
     */

    public JPanel createRankLabelsLeft() {
        JPanel rankLabels = new JPanel(new BorderLayout());

        rankLabels.add(createRanks(), BorderLayout.WEST);
        rankLabels.add(createBorder(), BorderLayout.EAST);

        return rankLabels;
    }

    /**
     * Displays the files to the bottom of the board
     * @return a JPanel displaying the files and the border displayed to the bottom of the board
     */

    public JPanel createFileLabelsBottom() {
        JPanel fileLabels = new JPanel(new BorderLayout());

        fileLabels.add(createFiles(), BorderLayout.SOUTH);
        fileLabels.add(createBorder(), BorderLayout.NORTH);

        fileLabels.setBorder(createCorner());

        return fileLabels;
    }

    /**
     * Displays the ranks to the right of the board
     * @return a JPanel displaying the ranks and the border displayed to the right of the board
     */

    public JPanel createRankLabelsRight() {
        JPanel rankLabels = new JPanel(new BorderLayout());

        rankLabels.add(createRanks(), BorderLayout.EAST);
        rankLabels.add(createBorder(), BorderLayout.WEST);

        return rankLabels;
    }

    /**
     * Displays the files to the top of the board
     * @return a JPanel displaying the files and the border displayed to the top of the board
     */

    public JPanel createFileLabelsTop() {
        JPanel fileLabels = new JPanel(new BorderLayout());

        fileLabels.add(createFiles(), BorderLayout.NORTH);
        fileLabels.add(createBorder(), BorderLayout.SOUTH);

        fileLabels.setBorder(createCorner());

        return fileLabels;
    }

    //________________________________________________Board Formatting Methods________________________________________________

    /**
     * Sets the background of a button, based on whether its a white or black square
     * @param coordinate the coordinate of the square being considered
     * @param b the button at the given square
     */

    private void backgroundSetter (Coordinate coordinate, JButton b){
        int signature = coordinate.getFile() - charFile + coordinate.getRank();
        if (signature % 2 == 0) {
            b.setBackground(brown);
        } else {
            b.setBackground(pastel);
        }
    }

    /**
     * Formats a JButton to be used in the board 2D array
     * @param coordinate the coordinate of the square being considered
     * @param pieces the pieces being used for the game
     * @return a JButton, correctly formatted with the piece it represents
     */

    private JButton setButton (Coordinate coordinate, Pieces pieces){
        JButton b = new JButton();

        Piece piece;

        backgroundSetter(coordinate, b);

        if (pieces.getPieces().get(coordinate) == null) {
            piece = null;
        } else {
            piece = pieces.getPiece(coordinate);
        }

        if (piece != null)
            b.setIcon(piece.getImageIcon());
        else
            b.setIcon(invisibleIcon);

        formatButton(b);

        return b;
    }

    /**
     * Correctly formats the properties of the JButton used in the boards array
     * @param b the JButton being formatted
     */

    private void formatButton (JButton b) {
        b.setSize(tileSize, tileSize);
        b.setOpaque(true);
        b.setContentAreaFilled(true);
        b.setBorderPainted(false);
        b.setVisible(true);
    }

    /**
     * Correctly formats the properties of an invisible JButton used in the boards array
     * @param b the JButton being formatted
     */

    public static void formatInvisibleButton (JButton b) {
        b.setSize(tileSize, tileSize);
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setVisible(true);
    }

    /**
     * Removes the ActionListener of all the JButtons of the board
     * This is done once a game is finished
     */

    private void disableBoardButtons() {
        for (int row = 0; row < dimension; row++) {
            for (int file = 0; file < dimension; file++) {
                board[row][file].removeActionListener(gameClick);
            }
        }
    }

    //________________________________________________Information Panel Method________________________________________________

    /**
     * The "Info Panel" is a JPanel containing information of the game (the moves being played)
     * and whether its check mate or a draw.
     * It contains the saveButton, used to save a game at any given time
     * @return a JPanel containing information on the game
     */

    private JPanel createInfoPanel() {
        JPanel movePanel = new JPanel(new GridBagLayout());
        movePanel.setBackground(infoColour);
        movePanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        movePanel.setPreferredSize(new Dimension(infoPanelWidth, 0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 0.4;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 5, 0);

        // Move history panel
        movePane.setEditable(false);
        movePane.setBackground(infoColour);
        movePane.setForeground(Color.WHITE);
        movePane.setFont(new Font("Arial", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(movePane);
        scrollPane.setBackground(infoColour);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        movePanel.add(scrollPane, gbc);

        // Game status panel
        gbc.gridy = 1;
        gbc.weighty = 0.1;
        matePane.setEditable(false);
        matePane.setBackground(infoColour);
        matePane.setForeground(Color.WHITE);
        matePane.setFont(new Font("Arial", Font.BOLD, 14));
        matePane.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        movePanel.add(matePane, gbc);
        
        // Add captured pieces panels
        gbc.gridy = 2;
        gbc.weighty = 0.25;
        gbc.insets = new Insets(5, 0, 5, 0);
        movePanel.add(whitePanel, gbc);
        
        gbc.gridy = 3;
        movePanel.add(blackPanel, gbc);

        // Save button
        gbc.gridy = 4;
        gbc.weighty = 0.0;
        gbc.insets = new Insets(10, 0, 0, 0);
        saveButton.setBackground(intermediate);
        saveButton.setForeground(Color.BLACK);
        saveButton.setFont(new Font("Arial", Font.BOLD, 12));
        saveButton.setPreferredSize(new Dimension(120, 30));
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(infoColour);
        buttonPanel.add(saveButton);
        movePanel.add(buttonPanel, gbc);

        return movePanel;
    }

    //________________________________________________Piece Movement Methods________________________________________________

    /**
     * Changes the turn of the game
     */

    private void setTurn() {
        turn = COLOUR.not(turn);
    }

    /**
     * Clears the counter. Used after a second click.
     * This is used for the logic of the game, as we can know when destination coordinate has been set,
     * or whether we have yet to choose and origin coordinate.
     */

    private void clearCounter() {
        counter = 0;
    }

    /**
     * This is used to illuminate the potential moves of the piece that is being clicked on
     * Capture positions are highlighted in red
     * @param potentials the potential moves of the piece being clicked on
     */
    private void processClick(HashSet<Coordinate> potentials) {
        for (int rank = 1; rank <= dimension; rank++) {
            for (char file = firstFile; file <= lastFile; file++) {
                int processedRank = rank - firstRank;
                int processedFile = file - firstFile;
                Coordinate potentialCoord = new Coordinate(file, rank);
                if (potentials.contains(potentialCoord)) {
                    // Check if the potential move is a capture
                    Piece pieceAtPosition = pieces.getPieces().get(potentialCoord);
                    if (pieceAtPosition != null && pieceAtPosition.getColour() != turn) {
                        board[processedRank][processedFile].setBackground(captureHighlight);
                    } else {
                        board[processedRank][processedFile].setBackground(intermediate);
                    }
                } else {
                    backgroundSetter(potentialCoord, board[processedRank][processedFile]);
                }
            }
        }
    }

    /**
     * This class is used to handle the game logic for the GUI.
     */

    private class ButtonHandle implements ActionListener {

        /**
         * Displays the potential moves, increases counter and sets the movingPiece to the piece selected
         * @param piece the piece selected
         */
        private void selectPiece(Piece piece) {
            processClick(piece.getPotentialMoves());
            counter++;
            movingPiece = piece;
        }

        /**
         * Used to turn 2D array "coordinates" into chess board coordinates
         * @param row the row of the 2D array
         * @param column the column of the 2D array
         * @return the Coordinate corresponding to an element of the board array with the given row and column
         */

        private Coordinate toCoordinate(int row, int column) {
            int rank = row + firstRank;
            char file = (char) (column + firstFile);

            return new Coordinate(file, rank);
        }

        /**
         * If the counter is at 0, and a button is clicked, we increase the counter and execute game click.
         * We also save the piece that has been clicked on, and assign it to movingPiece
         * If the counter is not at 0, we check the coordinate that has been selected.
         * If its a valid move for the movingPiece, then it executes makeMove on the pieces.
         * It also appends the move to str, changes the turn, reprints the board with the new pieces and clears the counter.
         * If it is an invalid move, then we consider the new clicked piece as the moving piece, and again, execute gameClick
         * If its mate, stalemate or draw, all the board buttons are disabled and the game ends.
         */

        @Override
        public void actionPerformed(ActionEvent actionEvent) {

            Coordinate coordinate = null;

            JButton source = (JButton) actionEvent.getSource();

            for (int i = 0; i < dimension; i++) {
                for (int j = 0; j < dimension; j++) {
                    if (board[i][j].equals(source)) {
                        coordinate = toCoordinate(i, j);
                    }
                }
            }

            Piece originPiece = pieces.getPieces().get(coordinate);

            if (coordinate != null && Coordinate.inBoard(coordinate)) {
                if (counter == 0) {
                    if (originPiece != null && originPiece.getColour() == turn) {
                        selectPiece(originPiece);
                    }
                } else {
                    Piece piece = movingPiece;

                    if (piece.isValidMove(coordinate, turn)) {
                        // Store the captured piece before making the move
                        Piece capturedPiece = null;
                        if (pieces.getPieces().get(coordinate) != null && 
                            pieces.getPieces().get(coordinate).getColour() != turn) {
                            capturedPiece = pieces.getPieces().get(coordinate);
                        }
                        
                        pieces.makeMove(coordinate, piece);
                        
                        // If a piece was captured, add its image to the captured pieces panel
                        if (capturedPiece != null) {
                            ImageIcon originalIcon = capturedPiece.getImageIcon();
                            Image scaledImage = originalIcon.getImage().getScaledInstance(
                                capturedPieceSize, capturedPieceSize, Image.SCALE_SMOOTH);
                            JLabel capturedLabel = new JLabel(new ImageIcon(scaledImage));
                            capturedLabel.setBorder(new EmptyBorder(1, 1, 1, 1));
                            
                            if (capturedPiece.getColour() == COLOUR.W) {
                                capturedBlackPiecesPanel.add(capturedLabel);
                                capturedBlackPiecesPanel.revalidate();
                                capturedBlackPiecesPanel.repaint();
                            } else {
                                capturedWhitePiecesPanel.add(capturedLabel);
                                capturedWhitePiecesPanel.revalidate();
                                capturedWhitePiecesPanel.repaint();
                            }
                        }
                        
                        if (turn == COLOUR.W) {
                            numberOfTurns++;
                            str.append(numberOfTurns).append(". ").append(ChessIO.moveString(pieces, coordinate, piece)).append(" ");
                        } else
                            str.append(ChessIO.moveString(pieces, coordinate, piece)).append(" ");
                        movePane.setText(str.toString());
                        for (int rank = 1; rank <= dimension; rank++) {
                            for (char file = firstFile; file <= lastFile; file++) {
                                int processedRank = rank - firstRank;
                                int processedFile = file - firstFile;
                                Coordinate potentialCoord = new Coordinate(file, rank);
                                backgroundSetter(potentialCoord, board[processedRank][processedFile]);
                                board[processedRank][processedFile].setIcon(invisibleIcon);
                                if (pieces.getPieces().get(potentialCoord) != null) {
                                    Piece updatePiece = pieces.getPiece(potentialCoord);
                                    board[processedRank][processedFile].setIcon(updatePiece.getImageIcon());
                                }
                            }
                        }

                        clearCounter();
                        setTurn();

                        if (pieces.isMate(turn)) {
                            matePane.setText(COLOUR.not(turn).toString() + " won by checkmate.");
                            disableBoardButtons();
                        } else if (pieces.isStalemate(COLOUR.not(turn))) {
                            matePane.setText("Draw by stalemate.");
                            disableBoardButtons();
                        } else if (pieces.isDraw()) {
                            matePane.setText("It's a draw.");
                            disableBoardButtons();
                        }
                    } else {
                        if (originPiece != null && originPiece.getColour() == turn) {
                            selectPiece(originPiece);
                        }
                    }
                }
            }
        }
    }

    //________________________________________________Game Saving Class________________________________________________

    /**
     * Handles the logic to save the game
     */

    public static class SaveHandle implements ActionListener {

        /**
         * This handles the saving of a game.
         * It produces a JOptionPane with an InputDialog.
         * We use ChessIO to validate the file path provided.
         * JOptionPane with message dialogs are then presented, based on whether the save was successful or not
         */

        @Override
        public void actionPerformed(ActionEvent actionEvent) {

            ImageIcon icon = new ImageIcon("WKing.png");

            UIManager.put("OptionPane.background", infoColour);
            UIManager.put("Panel.background", infoColour);
            UIManager.put("OptionPane.messageForeground", Color.white);

            String fileSave = (String) JOptionPane.showInputDialog(null,
                    "Enter a file name to save the game:",
                    "Save Game",
                    JOptionPane.INFORMATION_MESSAGE,
                    icon,null,null);

            if (fileSave != null) {

                String filePath = ChessIO.toTxt(fileSave);

                if (ChessIO.isErrorSave(filePath)) {
                    JOptionPane.showMessageDialog(null,
                            fileSave + " is an incorrect file name.",
                            "Failed Saving",
                            JOptionPane.ERROR_MESSAGE,
                            icon);
                } else {
                    if (ChessIO.saveGame(str.toString(), Paths.get(filePath)))
                        JOptionPane.showMessageDialog(null,
                                "Game saved succesfuly on path " + filePath,
                                "Save Succesful",
                                 JOptionPane.INFORMATION_MESSAGE,
                                 icon);
                    else
                        JOptionPane.showMessageDialog(null,
                                "There was an error saving the file on the path " + filePath + ". The name provided might be a duplicate.",
                                "Failed Saving",
                                JOptionPane.ERROR_MESSAGE,
                                icon);

                }
            }
        }
    }


    public static void main (String[]args){
        Pieces pieces = new Pieces();
        pieces.setGUIGame(true);
        new GUIBoard(pieces);
    }

}
