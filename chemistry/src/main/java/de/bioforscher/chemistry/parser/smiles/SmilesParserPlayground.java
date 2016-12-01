package de.bioforscher.chemistry.parser.smiles;

import de.bioforscher.chemistry.descriptive.elements.Element;
import de.bioforscher.chemistry.descriptive.elements.ElementProvider;
import de.bioforscher.chemistry.descriptive.molecules.MoleculeAtom;
import de.bioforscher.chemistry.descriptive.molecules.MoleculeBondType;
import de.bioforscher.chemistry.descriptive.molecules.MoleculeGraph;
import de.bioforscher.chemistry.descriptive.molecules.MoleculeGraphRenderer;
import de.bioforscher.core.utility.Pair;
import de.bioforscher.javafx.renderer.graphs.GraphDisplayApplication;
import javafx.application.Application;

import java.util.*;

import static de.bioforscher.chemistry.descriptive.molecules.MoleculeBondType.*;

/**
 * Created by Christoph on 17/11/2016.
 */
public class SmilesParserPlayground {


    public static void main(String[] args) {
        SmilesParserPlayground playground = new SmilesParserPlayground();
        // nested branches
        // String smilesString = "Nc1ncnc2n(cnc12)[C@@H]1O[C@H](COP(O)(=O)OP(O)(=O)OP(O)(O)=O)[C@@H](O)[C@H]1O";
        // simple without ring closure
        // String smilesString = "[H]C(=O)[C@H](O)[C@@H](O)[C@H](O)[C@H](O)CO";
        // simple with ring closure
        // String smilesString = "Clc(c(Cl)c(Cl)c1C(=O)O)c(Cl)c1Cl";
        // with ion
        String smilesString = "[H]C(=O)[C@H](O)[C@@H](O)[C@H](O)[C@H](O)COS([O-])(=O)=O";

        System.out.println(smilesString);
        playground.parse(smilesString);

        GraphDisplayApplication.graph = playground.graph;
        GraphDisplayApplication.renderer = new MoleculeGraphRenderer();
        Application.launch(GraphDisplayApplication.class);

    }

    private Queue<Character> queue;
    private Character currentSymbol;

    private List<String> tokens;
    private String currentToken;

    private MoleculeGraph graph;

    private int currentNeutronCount;
    // holds the charge of the current atom - this is reset to zero
    private int currentCharge;
    private Element currentElement;
    private Optional<MoleculeBondType> currentBondType;


    // maps the consecutive connections between atoms (as a pair of identifiers) to the bond type
    private HashMap<Pair<Integer>, MoleculeBondType> connectors;
    // maps the ring closures to the identifer of the first occurrence
    private HashMap<Integer, Integer> ringClosures;
    // holds the positions, where hydrogens should be added
    private List<Integer> hydrogens;

    private int currentIdentifer = Integer.MIN_VALUE;
    // remembers at which index a branch was opened
    private Deque<Integer> branches;
    // set true when a branch is closed so the previous connection is made
    private boolean firstAtomInBranch = false;
    // if a new chain opens directly after another closes the reference from the previous is not discarded
    private boolean sameChainReference;


    public SmilesParserPlayground() {
        this.queue = new LinkedList<>();
        this.tokens = new ArrayList<>();
        this.graph = new MoleculeGraph();
        this.connectors = new HashMap<>();
        this.ringClosures = new HashMap<>();
        this.branches = new ArrayDeque<>();
        this.hydrogens = new ArrayList<>();
        this.currentToken = "";
        this.currentBondType = Optional.empty();
    }

    private void parse(String smilesString) {
        // SMILES   ::= Atom ( Chain | Branch )*

        for (char aChar : smilesString.toCharArray()) {
            this.queue.add(aChar);
        }

        this.currentSymbol = this.queue.poll();

        while (!this.queue.isEmpty()) {
            if (!parseSmiles()) {
                throw new IllegalArgumentException("The given string is no valid SMILES String (Exception was thrown" +
                        " after " + this.tokens + " have been parsed).");
            }
        }

        this.tokens.forEach(s -> System.out.print(s + " "));
        System.out.println();
        this.graph.getNodes().stream()
                .sorted(Comparator.comparing(MoleculeAtom::getIdentifier))
                .forEach(atom -> System.out.print(atom + " "));
        System.out.println();
        this.connectors.forEach((key, value) -> System.out.println(key + " - " + value));
        System.out.println();
        this.ringClosures.forEach((key, value) -> System.out.println(key + " - " + value));

        // add hydrogens to connectors
        this.hydrogens.forEach((identifier) -> {
            int hydrogenIdentifier = this.graph.addNextAtom("H");
            this.connectors.put(new Pair<>(identifier, hydrogenIdentifier), SINGLE_BOND);
        });


        this.connectors.forEach((connector, type) ->
                this.graph.addEdgeBetween(this.graph.getNode(connector.getFirst()), this.graph.getNode(connector.getSecond()), type));

    }

    private boolean parseSmiles() {
        // SMILES ::= Atom ( Chain | Branch )*
        if (isEmpty()) {
            return false;
        }

        if (!parseAtom()) {
            return false;
        }

        boolean parsable;
        do {
            parsable = parseBranch();
            parsable |= parseChain();
        } while (parsable);

        return true;
    }

    private boolean parseAtom() {
        if (isEmpty()) {
            return false;
        }

        if (parseOrganicSymbol(false)) {
            connectConsecutiveAtoms();
            return true;
        } else if (parseAromaticSymbol(false)) {
            connectConsecutiveAtoms();
            return true;
        } else if (parseAtomSpecification()) {
            connectConsecutiveAtoms();
            return true;
        } else if (this.currentSymbol == '*') {
            addToTokens();
            poll();
        }
        return false;
    }

    private boolean parseBranch() {
        // Branch ::= '(' Bond? SMILES+ ')'
        if (isEmpty()) {
            return false;
        }

        if (this.currentSymbol == '(') {
            addToTokens();
            poll();
            openBranch();
            parseBond();

            int length = 0;
            while (parseSmiles()) {
                length++;
            }

            if (this.currentSymbol == ')') {
                // ending with closed round brackets
                if (length < 1) {
                    return false;
                }
                closeBranch();
                addToTokens();
                poll();
                return true;
            }
        }
        return false;
    }

    private boolean parseChain() {
        if (isEmpty()) {
            return false;
        }

        boolean parsable;
        int length = 0;
        do {
            parseBond();
            parsable = parseAtom();
            parsable |= parseRingClosure();
            if (parsable) {
                length++;
            }
        } while (parsable);
        return length > 0;
    }

    private boolean parseBond() {
        if (isEmpty()) {
            return false;
        }

        switch (this.currentSymbol) {
            case '-':
            case '=':
            case '#':
            case '$':
            case ':':
            case '/':
            case '\\': {
                addToTokens();
                setNextBond();
                poll();
                return true;
            }
            case '.': {
                // don't connect
                addToTokens();
                poll();
                return true;
            }
            default: {
                return false;
            }
        }

    }

    private boolean parseRingClosure() {
        if (isEmpty()) {
            return false;
        }

        if (this.currentSymbol == '%') {
            if (isNonZeroDecimal()) {
                addToCurrentToken();
                poll();
                if (isDecimal()) {
                    addToCurrentToken();
                    poll();
                    return true;
                }
            }
        } else if (isDecimal()) {
            addToTokens();
            addRingClosure();
            poll();
            return true;
        }
        return false;
    }

    private boolean parseOrganicSymbol(boolean addLater) {
        if (isEmpty()) {
            return false;
        }

        // OrganicSymbol ::= 'B' 'r'? | 'C' 'l'? | 'N' | 'O' | 'P' | 'S' | 'F' | 'I'
        switch (this.currentSymbol) {
            case 'B': {
                if (this.queue.peek() == 'r') {
                    // Brom
                    dispose();
                    if (addLater) {
                        this.currentToken += "Br";
                        this.currentElement = ElementProvider.getElementBySymbol(String.valueOf("Br"));
                    } else {
                        this.tokens.add("Br");
                        addAtomToGraph("Br");
                    }
                } else {
                    // Bor
                    handleAtom(addLater);
                }
                poll();
                return true;
            }
            case 'C': {
                if (this.queue.peek() == 'l') {
                    // Chlor
                    dispose();
                    if (addLater) {
                        this.currentToken += "Cl";
                        this.currentElement = ElementProvider.getElementBySymbol(String.valueOf("Cl"));
                    } else {
                        this.tokens.add("Cl");
                        addAtomToGraph("Cl");
                    }
                } else {
                    // Carbon
                    handleAtom(addLater);
                }
                poll();
                return true;
            }
            case 'N':
            case 'O':
            case 'P':
            case 'S':
            case 'F':
            case 'I': {
                handleAtom(addLater);
                poll();
                return true;
            }
            default: {
                return false;
            }

        }
    }

    private void handleAtom(boolean addLater) {
        if (addLater) {
            addToCurrentToken();
            this.currentElement = ElementProvider.getElementBySymbol(String.valueOf(this.currentSymbol));
        } else {
            addToTokens();
            addAtomToGraph(this.currentSymbol);
        }
    }

    private boolean parseAromaticSymbol(boolean addLater) {
        if (isEmpty()) {
            return false;
        }

        // AromaticSymbol ::= 'b' | 'c' | 'n' | 'o' | 'p' | 's'
        switch (this.currentSymbol) {
            case 'b':
            case 'c':
            case 'n':
            case 'o':
            case 'p':
            case 's': {
                if (addLater) {
                    addToCurrentToken();
                    this.currentElement = ElementProvider.getElementBySymbol(String.valueOf(this.currentSymbol));
                    this.currentBondType = Optional.of(MoleculeBondType.AROMATIC_BOND);
                } else {
                    addToTokens();
                    addAtomToGraph(this.currentSymbol);
                }
                // todo take care of the aromatic bond
                poll();
                return true;
            }
            default: {
                return false;
            }
        }
    }

    private boolean parseAtomSpecification() {
        if (isEmpty()) {
            return false;
        }

        // AtomSpec ::= '[' Isotope? ( 'se' | 'as' | AromaticSymbol | ElementSymbol | WILDCARD ) ChiralClass? HCount? Charge? Class? ']'
        if (this.currentSymbol == '[') {
            addToCurrentToken();
            poll();
            // try to parse isotope
            parseIsotope();
            // one of the following has to match!
            // try to parse aromatic selene or arsenic
            if (!parseAromaticSeleniumAndArsenic()) {
                // if not, try to parse other aromatic elements
                if (!parseAromaticSymbol(true)) {
                    // if not, try to parse element
                    if (!parseElementSymbol()) {
                        // if not, try to parse wildcard
                        if (this.currentSymbol == '*') {
                            addToCurrentToken();
                            poll();
                        } else {
                            // if all of the previous fail the smiles is invalid
                            return false;
                        }
                    }
                }
            }

            parseChirality();
            parseHCount();
            parseCharge();
            parseClass();

            addAtom();

            if (this.currentSymbol == ']') {
                // ending with closed square brackets
                addToCurrentToken();
                addAndClearCurrentToken();
                poll();
                return true;
            }
        }
        return false;
    }

    private boolean parseIsotope() {
        if (isEmpty()) {
            return false;
        }

        // Isotope  ::= [1-9] [0-9]? [0-9]?
        if (isNonZeroDecimal()) {
            // parse first nonzero decimal
            addToCurrentToken();
            poll();
            if (isDecimal()) {
                // parse second decimal
                addToCurrentToken();
                poll();
                if (isDecimal()) {
                    // parse third decimal
                    addToCurrentToken();
                    poll();
                }
            }
            return true;
        }
        return false;

    }

    private boolean parseAromaticSeleniumAndArsenic() {
        if (isEmpty()) {
            return false;
        }

        if (this.currentSymbol == 's') {
            if (this.queue.peek() == 'e') {
                // parse selenium
                dispose();
                this.currentElement = ElementProvider.getElementBySymbol(String.valueOf("Se"));
                this.currentBondType = Optional.of(MoleculeBondType.AROMATIC_BOND);
                poll();
                return true;
            }
            return false;
        } else if (this.currentSymbol == 'a') {
            if (this.queue.peek() == 's') {
                // parse arsenic
                dispose();
                this.currentElement = ElementProvider.getElementBySymbol(String.valueOf("As"));
                this.currentBondType = Optional.of(MoleculeBondType.AROMATIC_BOND);
                poll();
                return true;
            }
            return false;
        }
        return false;
    }

    private boolean parseElementSymbol() {
        // ElementSymbol ::= [A-Z] [a-z]?
        if (isUpperCaseWordCharacter()) {
            String element = "";
            addToCurrentToken();
            element += this.currentSymbol;
            poll();
            if (isLowerCaseWordCharacter()) {
                addToCurrentToken();
                element += this.currentSymbol;
                poll();
            }
            this.currentElement = ElementProvider.getElementBySymbol(String.valueOf(element));
            return true;
        }
        return false;
    }

    private boolean parseChirality() {
        if (isEmpty()) {
            return false;
        }

        // ChiralClass ::= ( '@' ( '@' | 'TH' [1-2] | 'AL' [1-2] | 'SP' [1-3] |
        // 'TB' ( '1' [0-9]? | '2' '0'? | [3-9] ) | 'OH' ( '1' [0-9]? | '2' [0-9]? | '3' '0'? | [4-9] ) )? )?
        if (this.currentSymbol == '@') {
            addToCurrentToken();
            poll();
            if (this.currentSymbol == '@') {
                addToCurrentToken();
                poll();
                return true;
            } else if (this.currentSymbol == 'T') {
                if (this.queue.peek() == 'H') {
                    addThisAndNext();
                    poll();
                    if (isInRage('1', '2')) {
                        addToCurrentToken();
                        poll();
                        return true;
                    }
                } else if (this.queue.peek() == 'B') {
                    addThisAndNext();
                    poll();
                    if (this.currentSymbol == '1') {
                        addToCurrentToken();
                        poll();
                        if (isDecimal()) {
                            addToCurrentToken();
                            poll();
                        }
                        return true;
                    } else if (this.currentSymbol == '2') {
                        addToCurrentToken();
                        poll();
                        if (this.currentSymbol == '0') {
                            addToCurrentToken();
                            poll();
                        }
                        return true;
                    } else if (isInRage('3', '9')) {
                        addToCurrentToken();
                        poll();
                        return true;
                    }
                }
            } else if (this.currentSymbol == 'A') {
                if (this.queue.peek() == 'L') {
                    addThisAndNext();
                    poll();
                    if (isInRage('1', '2')) {
                        addToCurrentToken();
                        poll();
                        return true;
                    }
                }
            } else if (this.currentSymbol == 'S') {
                if (this.queue.peek() == 'P') {
                    addThisAndNext();
                    poll();
                    if (isInRage('1', '3')) {
                        addToCurrentToken();
                        poll();
                        return true;
                    }
                }
            } else if (this.currentSymbol == 'O') {
                if (this.queue.peek() == 'H') {
                    addThisAndNext();
                    poll();
                    if (this.currentSymbol == '1') {
                        addToCurrentToken();
                        poll();
                        if (isDecimal()) {
                            addToCurrentToken();
                            poll();
                        }
                        return true;
                    } else if (this.currentSymbol == '2') {
                        addToCurrentToken();
                        poll();
                        if (isDecimal()) {
                            addToCurrentToken();
                            poll();
                        }
                        return true;
                    } else if (this.currentSymbol == '3') {
                        addToCurrentToken();
                        poll();
                        if (this.currentSymbol == '0') {
                            addToCurrentToken();
                            poll();
                        }
                        return true;
                    } else if (isInRage('4', '9')) {
                        addToCurrentToken();
                        poll();
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean parseHCount() {
        if (isEmpty()) {
            return false;
        }

        // HCount   ::= 'H' [0-9]?
        if (this.currentSymbol == 'H') {
            addToCurrentToken();
            poll();
            if (isDecimal()) {
                addToCurrentToken();
                connectHydrogens(Integer.valueOf(String.valueOf(this.currentSymbol)));
                poll();
                return true;
            }
            connectHydrogens(1);
        }
        return false;
    }

    private boolean parseCharge() {
        if (isEmpty()) {
            return false;
        }
        String chargeToken = "";
        // Charge   ::= '-' ( '-' | '0' | '1' [0-5]? | [2-9] )?
        //            | '+' ( '+' | '0' | '1' [0-5]? | [2-9] )?
        if (this.currentSymbol == '+') {
            chargeToken += this.currentSymbol;
            addToCurrentToken();
            poll();
            if (this.currentSymbol == '+') {
                chargeToken += this.currentSymbol;
                addToCurrentToken();
                poll();
            } else {
                chargeToken += parseChargeNumber();
            }
            setCharge(chargeToken);
            return true;
        } else if (this.currentSymbol == '-') {
            chargeToken += this.currentSymbol;
            addToCurrentToken();
            poll();
            if (this.currentSymbol == '-') {
                chargeToken += this.currentSymbol;
                addToCurrentToken();
                poll();
            } else {
                chargeToken += parseChargeNumber();
            }
            setCharge(chargeToken);
            return true;
        }
        return false;
    }

    private String parseChargeNumber() {
        String chargeToken = "";
        if (this.currentSymbol == '0') {
            chargeToken += this.currentSymbol;
            addToCurrentToken();
            poll();
        } else if (this.currentSymbol == '1') {
            chargeToken += this.currentSymbol;
            addToCurrentToken();
            poll();
            if (isInRage('0', '5')) {
                chargeToken += this.currentSymbol;
                addToCurrentToken();
                poll();
            }
        } else if (isInRage('2', '9')) {
            chargeToken += this.currentSymbol;
            addToCurrentToken();
            poll();
        }
        return chargeToken;
    }

    private void setCharge(String chargeToken) {
        switch (chargeToken) {
            case "+":
                this.currentCharge = 1;
                break;
            case "++":
                this.currentCharge = 2;
                break;
            case "-":
                this.currentCharge = -1;
                break;
            case "--":
                this.currentCharge = -2;
                break;
            default:
                this.currentCharge = Integer.valueOf(chargeToken);
                break;
        }
    }

    private boolean parseClass() {
        if (isEmpty()) {
            return false;
        }
        // Class    ::= ':' [0-9]+
        if (this.currentSymbol == ':') {
            addToCurrentToken();
            poll();
            int length = 0;
            while (isDecimal()) {
                addToCurrentToken();
                poll();
                length++;
            }
            return length > 0;
        }
        return false;
    }

    private void setNextBond() {
        this.currentBondType = Optional.of(getBondForSMILESSymbol(this.currentSymbol));
    }

    private void connectConsecutiveAtoms() {
        if (this.graph.getNodes().size() > 1) {
            if (this.firstAtomInBranch) {
                if (this.sameChainReference) {
                    this.connectors.put(new Pair<>(this.branches.peekLast(), this.currentIdentifer), this.currentBondType.orElse(SINGLE_BOND));
                    this.sameChainReference = false;
                } else {
                    this.connectors.put(new Pair<>(this.branches.pollLast(), this.currentIdentifer), this.currentBondType.orElse(SINGLE_BOND));
                }
                this.currentBondType = Optional.empty();
                this.firstAtomInBranch = false;
            } else {
                this.connectors.put(new Pair<>(this.currentIdentifer - 1, this.currentIdentifer), this.currentBondType.orElse(SINGLE_BOND));
                this.currentBondType = Optional.empty();
            }
        }
    }

    private void connectAromaticAtoms() {
        this.connectors.put(new Pair<>(this.currentIdentifer - 1, this.currentIdentifer), AROMATIC_BOND);
    }

    private void connectHydrogens(int hydrogenCount) {
        for (int count = 0; count < hydrogenCount; count++) {
            this.hydrogens.add(this.currentIdentifer+1);
        }
    }

    private void addRingClosure() {
        int closureIdentifier = Integer.valueOf(String.valueOf(this.currentSymbol));
        if (this.ringClosures.containsKey(closureIdentifier)) {
            this.connectors.put(new Pair<>(this.ringClosures.get(closureIdentifier), this.currentIdentifer), SINGLE_BOND);
            this.ringClosures.remove(closureIdentifier);
        } else {
            this.ringClosures.put(closureIdentifier, this.currentIdentifer);
        }
    }

    private boolean isEmpty() {
        return this.currentSymbol == null;
    }

    private boolean isInRage(Character rangeStart, Character rangeEnd) {
        return this.currentSymbol >= rangeStart && this.currentSymbol <= rangeEnd;
    }

    /**
     * Checks if the current symbol is a decimal character, but not zero [1-9]
     *
     * @return {@code true} if the current symbol is a decimal character, but not zero.
     */
    private boolean isNonZeroDecimal() {
        return isInRage('1', '9');
    }

    /**
     * Checks if the current symbol is a decimal character [0-9]
     *
     * @return {@code true} if the current symbol is a decimal character.
     */
    private boolean isDecimal() {
        return isInRage('0', '9');
    }

    /**
     * Checks if the current symbol is a lowercase word character [a-z]
     *
     * @return {@code true} if the current symbol is a lowercase word character.
     */
    private boolean isLowerCaseWordCharacter() {
        return isInRage('a', 'z');
    }

    /**
     * Checks if the current symbol is a uppercase word character [A-Z]
     *
     * @return {@code true} if the current symbol is a uppercase word character.
     */
    private boolean isUpperCaseWordCharacter() {
        return isInRage('A', 'Z');
    }

    /**
     * Adds the current symbol to the current token.
     */
    private void addToCurrentToken() {
        this.currentToken += this.currentSymbol;
    }

    private void addThisAndNext() {
        addToCurrentToken();
        poll();
        addToCurrentToken();
    }

    /**
     * Adds the current symbol to the collected tokens.
     */
    private void addToTokens() {
        this.tokens.add(String.valueOf(this.currentSymbol));
    }

    private void addAndClearCurrentToken() {
        this.tokens.add(this.currentToken);
        this.currentToken = "";
    }

    /**
     * Polls the next character from the queue and sets the current symbol
     */
    private void poll() {
        this.currentSymbol = this.queue.poll();
    }

    /**
     * Disposes the current character.
     */
    private void dispose() {
        this.poll();
    }

    /**
     * Adds a atom to the graph. This method creates a new atom with the specified symbol({@link Element#getSymbol()})
     *
     * @param atom The symbol of the element of the new atom.
     */
    private void addAtomToGraph(char atom) {
        addAtomToGraph(String.valueOf(atom));
    }

    /**
     * Adds a atom to the graph. This method creates a new atom with the specified symbol({@link Element#getSymbol()})
     *
     * @param atom The symbol of the element of the new atom.
     */
    private void addAtomToGraph(String atom) {
        this.currentIdentifer = this.graph.addNextAtom(atom);
    }

    private void addAtom() {
        this.currentIdentifer = this.graph.addNextAtom(this.currentElement, this.currentCharge);
        this.currentElement = null;
        this.currentCharge = 0;
    }

    private void openBranch() {
        if (!this.sameChainReference) {
            this.branches.add(this.currentIdentifer);
        }

    }

    private void closeBranch() {
        if (this.queue.peek() == '(') {
            this.sameChainReference = true;
        }
        this.firstAtomInBranch = true;
    }

}
