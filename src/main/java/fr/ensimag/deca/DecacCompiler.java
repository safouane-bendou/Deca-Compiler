package fr.ensimag.deca;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.ObjectInputStream.GetField;
import java.util.Map;
import java.lang.String;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.apache.log4j.Logger;
import fr.ensimag.deca.context.BooleanType;
import fr.ensimag.deca.context.ClassDefinition;
import fr.ensimag.deca.context.ClassType;
import fr.ensimag.deca.context.Definition;
import fr.ensimag.deca.context.EnvironmentExp;
import fr.ensimag.deca.context.FloatType;
import fr.ensimag.deca.context.IntType;
import fr.ensimag.deca.context.MethodDefinition;
import fr.ensimag.deca.context.NullType;
import fr.ensimag.deca.context.Signature;
import fr.ensimag.deca.context.StringType;
import fr.ensimag.deca.context.TypeDefinition;
import fr.ensimag.deca.context.VoidType;
import fr.ensimag.deca.syntax.DecaLexer;
import fr.ensimag.deca.syntax.DecaParser;
import fr.ensimag.deca.tools.DecacInternalError;
import fr.ensimag.deca.tools.IndentPrintStream;
import fr.ensimag.deca.tools.SymbolTable;
import fr.ensimag.deca.tools.SymbolTable.Symbol;
import fr.ensimag.deca.tree.AbstractProgram;
import fr.ensimag.deca.tree.Location;
import fr.ensimag.deca.tree.LocationException;
import fr.ensimag.ima.pseudocode.AbstractLine;
import fr.ensimag.ima.pseudocode.IMAProgram;
import fr.ensimag.ima.pseudocode.Instruction;
import fr.ensimag.ima.pseudocode.Label;

/**
 * Decac compiler instance.
 *
 * This class is to be instantiated once per source file to be compiled. It
 * contains the meta-data used for compiling (source file name, compilation
 * options) and the necessary utilities for compilation (symbol tables, abstract
 * representation of target file, ...).
 *
 * It contains several objects specialized for different tasks. Delegate methods
 * are used to simplify the code of the caller (e.g. call
 * compiler.addInstruction() instead of compiler.getProgram().addInstruction()).
 *
 * @author gl46
 * @date 01/01/2022
 */
public class DecacCompiler {
    private static final Logger LOG = Logger.getLogger(DecacCompiler.class);
    private EnvironmentExp envTypes= new EnvironmentExp(null);

    /**
     * Portable newline character.
     */
    private static final String nl = System.getProperty("line.separator", "\n");
    
    //public SymbolTable getSymbolTable(){
      //  return table;
    //}
    
    private Label labelDebutProg = new Label("debutProg");
    private Label labelDivErreur = new Label("erreurDiv0");
    public DecacCompiler(CompilerOptions compilerOptions, File source) {
        super();
        this.compilerOptions = compilerOptions;
        this.source = source;
        Map<Symbol,Definition> typeEnv = envTypes.getCurrentEnvironment();
        typeEnv.put(DecaParser.tableSymb.create("void"), new TypeDefinition(new VoidType(DecaParser.tableSymb.create("void")), new Location(0,0," ")));
        typeEnv.put(DecaParser.tableSymb.create("boolean"),new TypeDefinition(new BooleanType(DecaParser.tableSymb.create("boolean")), new Location(0,0," ")));
        typeEnv.put(DecaParser.tableSymb.create("float"),new TypeDefinition(new FloatType(DecaParser.tableSymb.create("float")), new Location(0,0," ")));
        typeEnv.put(DecaParser.tableSymb.create("int"),new TypeDefinition(new IntType(DecaParser.tableSymb.create("int")), new Location(0,0," ")));
        typeEnv.put(DecaParser.tableSymb.create("string"),new TypeDefinition(new StringType(DecaParser.tableSymb.create("string")), new Location(0,0," ")));
        typeEnv.put(DecaParser.tableSymb.create("null"),new TypeDefinition(new NullType(DecaParser.tableSymb.create("null")), new Location(0,0," ")));
        ClassDefinition objetDef=new ClassDefinition(new ClassType(DecaParser.tableSymb.create("Object"),new Location(0, 0, " "),null),new Location(0, 0, " "), null);
        Map<Symbol,Definition> objEnvironment =objetDef.getMembers().getCurrentEnvironment();
        Signature equalsSignature= new Signature();
        equalsSignature.add(new BooleanType(DecaParser.tableSymb.create("boolean")));
        equalsSignature.add(new ClassType(DecaParser.tableSymb.create("Object"),new Location(0, 0, " "),null));
        MethodDefinition MD = new MethodDefinition(new BooleanType(DecaParser.tableSymb.create("boolean")), new Location(0, 0, " "),equalsSignature, 0);
        MD.setLabel(new Label("code.Object.equals"));
        objEnvironment.put(DecaParser.tableSymb.create("equals"),MD);
        objetDef.setNumberOfMethods(1);
        typeEnv.put(DecaParser.tableSymb.create("Object"),objetDef);
        envTypes.setCurrentEnvironment(typeEnv);
    }


    public Label getLabelDebutProg() {
        return labelDebutProg;
    }


    public Label getLabelDivErreur() {
        return labelDivErreur;
    }


    public EnvironmentExp getEnvTypes(){
        return this.envTypes;
    }
    /**
     * Source file associated with this compiler instance.
     */
    public File getSource() {
        return source;
    }

    /**
     * Compilation options (e.g. when to stop compilation, number of registers
     * to use, ...).
     */
    public CompilerOptions getCompilerOptions() {
        return compilerOptions;
    }

    /**
     * @see
     * fr.ensimag.ima.pseudocode.IMAProgram#add(fr.ensimag.ima.pseudocode.AbstractLine)
     */
    public void add(AbstractLine line) {
        program.add(line);
    }

    /**
     * @see fr.ensimag.ima.pseudocode.IMAProgram#addComment(java.lang.String)
     */
    public void addComment(String comment) {
        program.addComment(comment);
    }

    /**
     * @see
     * fr.ensimag.ima.pseudocode.IMAProgram#addLabel(fr.ensimag.ima.pseudocode.Label)
     */
    public void addLabel(Label label) {
        program.addLabel(label);
    }

    /**
     * @see
     * fr.ensimag.ima.pseudocode.IMAProgram#addInstruction(fr.ensimag.ima.pseudocode.Instruction)
     */
    public void addInstruction(Instruction instruction) {
        program.addInstruction(instruction);
    }

    /**
     * @see
     * fr.ensimag.ima.pseudocode.IMAProgram#addInstruction(fr.ensimag.ima.pseudocode.Instruction,
     * java.lang.String)
     */
    public void addInstruction(Instruction instruction, String comment) {
        program.addInstruction(instruction, comment);
    }
    
    /**
     * @see 
     * fr.ensimag.ima.pseudocode.IMAProgram#display()
     */
    public String displayIMAProgram() {
        return program.display();
    }
    
    private final CompilerOptions compilerOptions;
    private final File source;
    /**
     * The main program. Every instruction generated will eventually end up here.
     */
    private final IMAProgram program = new IMAProgram();
 

    /**
     * Run the compiler (parse source file, generate code)
     *
     * @return true on error
     */
    public boolean compile() {
        String sourceFile = source.getAbsolutePath();
        String destFile = sourceFile.substring(0,sourceFile.lastIndexOf('.'))+".ass";
        // A FAIRE: calculer le nom du fichier .ass à partir du nom du
        // A FAIRE: fichier .deca.
        //System.out.println(sourceFile);
        //destFile = "test.ass";
        PrintStream err = System.err;
        PrintStream out = System.out;
        LOG.debug("Compiling file " + sourceFile + " to assembly file " + destFile);
        try {
            return doCompile(sourceFile, destFile, out, err);
        } catch (LocationException e) {
            e.display(err);
            return true;
        } catch (DecacFatalError e) {
            err.println(e.getMessage());
            return true;
        } catch (StackOverflowError e) {
            LOG.debug("stack overflow", e);
            err.println("Stack overflow while compiling file " + sourceFile + ".");
            return true;
        } catch (Exception e) {
            LOG.fatal("Exception raised while compiling file " + sourceFile
                    + ":", e);
            err.println("Internal compiler error while compiling file " + sourceFile + ", sorry.");
            return true;
        } catch (AssertionError e) {
            LOG.fatal("Assertion failed while compiling file " + sourceFile
                    + ":", e);
            err.println("Internal compiler error while compiling file " + sourceFile + ", sorry.");
            return true;
        }
    }

    /**
     * Internal function that does the job of compiling (i.e. calling lexer,
     * verification and code generation).
     *
     * @param sourceName name of the source (deca) file
     * @param destName name of the destination (assembly) file
     * @param out stream to use for standard output (output of decac -p)
     * @param err stream to use to display compilation errors
     *
     * @return true on error
     */
    private boolean doCompile(String sourceName, String destName,
            PrintStream out, PrintStream err)
            throws DecacFatalError, LocationException {
        AbstractProgram prog = doLexingAndParsing(sourceName, err);
        CompilerOptions options = this.getCompilerOptions();
        if (prog == null) {
            LOG.info("Parsing failed");
            return true;
        }
        if(options.getParse()){
            PrintStream printStream = new PrintStream(System.out);
            IndentPrintStream stream = new IndentPrintStream(printStream);
            prog.decompile(stream);
            return false;
        }

        assert(prog.checkAllLocations());


        prog.verifyProgram(this);
        assert(prog.checkAllDecorations());
        if(options.getVerification()){
            //return true;
        }
        addComment("start main program");
        prog.codeGenProgram(this);
        addComment("end main program");
        LOG.debug("Generated assembly code:" + nl + program.display());
        LOG.info("Output file assembly file is: " + destName);

        FileOutputStream fstream = null;
        try {
            fstream = new FileOutputStream(destName);
        } catch (FileNotFoundException e) {
            throw new DecacFatalError("Failed to open output file: " + e.getLocalizedMessage());
        }

        LOG.info("Writing assembler file ...");

        program.display(new PrintStream(fstream));
        LOG.info("Compilation of " + sourceName + " successful.");
        return false;
    }

    /**
     * Build and call the lexer and parser to build the primitive abstract
     * syntax tree.
     *
     * @param sourceName Name of the file to parse
     * @param err Stream to send error messages to
     * @return the abstract syntax tree
     * @throws DecacFatalError When an error prevented opening the source file
     * @throws DecacInternalError When an inconsistency was detected in the
     * compiler.
     * @throws LocationException When a compilation error (incorrect program)
     * occurs.
     */
    protected AbstractProgram doLexingAndParsing(String sourceName, PrintStream err)
            throws DecacFatalError, DecacInternalError {
        DecaLexer lex;
        try {
            lex = new DecaLexer(CharStreams.fromFileName(sourceName));
        } catch (IOException ex) {
            throw new DecacFatalError("Failed to open input file: " + ex.getLocalizedMessage());
        }
        lex.setDecacCompiler(this);
        CommonTokenStream tokens = new CommonTokenStream(lex);
        DecaParser parser = new DecaParser(tokens);
        parser.setDecacCompiler(this);
        return parser.parseProgramAndManageErrors(err);
    }

    public Label addInstruction(Label label) {
        return null;
    }

}

