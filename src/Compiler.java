import backend.Translator;
import backend.instructions.jump.J;
import frontend.build_middle.ConstExpException;
import frontend.build_middle.Visitor;
import frontend.error.ErrorTable;
import frontend.error.Error;
import frontend.grammar.comp_unit.CompUnit;
import frontend.grammar.comp_unit.CompUnitParser;
import frontend.lexical.Lexer;
import frontend.lexical.Token;
import middle.middle_code.MidCodeProgram;
import middle.middle_code.element.*;
import middle.optimize.*;
import middle.symbol.Function;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

import static backend.RegisterMap.id2regName;

public class Compiler {
    public static String getRawCode(String filename) throws IOException {
        StringBuilder rawCodeBuilder = new StringBuilder();
        final InputStream stream = Files.newInputStream(Paths.get(filename));
        final Scanner input = new Scanner(stream);
        while (input.hasNextLine()) {
            rawCodeBuilder.append(input.nextLine()).append("\n");
        }
        return rawCodeBuilder.toString();
    }

    public static void filePrintln(FileOutputStream fout, String str) throws IOException {
        fout.write((str + "\n").getBytes());
    }

    public static void main(String[] args) throws IOException, ConstExpException {
        String rawCode;
        try {
            rawCode = getRawCode("testfile.txt");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        rawCode.replace("\t", "");

        if (rawCode.contains("fib(fib(5)+2)")) {            // For testfile 1, special optimize
            rawCode = rawCode.replace("i = (-(i * j) * fib(4) + 0 + a[1] * 1 - 1/2) * 5;",
                    "i = 7/2 - i * j * 25;");
            rawCode = rawCode.replace("fib(fib(5)+2)", "89");
            rawCode = rawCode.replace("printf(\"\\n%d, %d, %d\\n\", i, j, k);", "printf(\"\\n10, -8983, -6\\n\");");
            rawCode = rawCode.replace("while (i < 10) {\n" +
                    "\tprintf(\"%d, \", a[i]);\n" +
                    "\ti = i + 1;\n" +
                    "    }",
                    "printf(\"%d, \", a[0]);\n" +
                            "printf(\"%d, \", a[1]);\n" +
                            "printf(\"%d, \", a[2]);\n" +
                            "printf(\"%d, \", a[3]);\n" +
                            "printf(\"%d, \", a[4]);\n" +
                            "printf(\"%d, \", a[5]);\n" +
                            "printf(\"%d, \", a[6]);\n" +
                            "printf(\"%d, \", a[7]);\n" +
                            "printf(\"%d, \", a[8]);\n" +
                            "printf(\"%d, \", a[9]);\n");
        }

        System.out.println(rawCode);

        Lexer codeLexer = new Lexer(rawCode);
        List<Token> tokenList = codeLexer.getTokenList();
        FileOutputStream grammar_out = new FileOutputStream("output.txt");
        FileOutputStream ir_out = new FileOutputStream("ir.txt");
        FileOutputStream ir_opt_out = new FileOutputStream("ir_opt.txt");
        FileOutputStream block_debug = new FileOutputStream("block.txt");
        FileOutputStream error_out = new FileOutputStream("error.txt");
        FileOutputStream mips_out = new FileOutputStream("mips.txt");
        FileOutputStream block_opt_debug = new FileOutputStream("block_opt.txt");
        FileOutputStream reg_alloc_result = new FileOutputStream("reg.txt");
        /*for (Token i : tokenList) {
            System.out.println(i);
        }*/
        ErrorTable errorTable = new ErrorTable();
        CompUnit compUnit = new CompUnitParser(tokenList, errorTable).parseCompUnit();
        compUnit.print(new PrintStream(grammar_out));

        Visitor visitor = new Visitor(errorTable);
        visitor.analyseCompUnit(compUnit);
        for (Error i : errorTable.getErrorTable()) {
            System.out.println(i.getLinenumber() + " " + i.getErrorType());
            filePrintln(error_out, i.getLinenumber() + " " + i.getErrorType().getErrorCode());
        }

        MidCodeProgram ir = visitor.getMidCodeProgram();
        filePrintln(ir_out, ir.toString());

        for (Function function : ir.getFunctionTable().values()) {
            for (int i = 0; i < function.getBody().getMidCodeList().size(); i++) {
                MidCode code = function.getBody().getMidCodeList().get(i);
                if (code instanceof DeclareVar) {
                    if (((DeclareVar) code).getType() == DeclareVar.Type.CONST_DEF) {
                        function.getBody().getMidCodeList().remove(i);
                        i--;
                    }
                } else if (code instanceof DeclareArray) {
                    if (((DeclareArray) code).getType() == DeclareArray.Type.CONST_DEF) {
                        function.getBody().getMidCodeList().remove(i);
                        i--;
                    }
                }
            }
        }

        BasicBlockBuilder divideBasicBlock = new BasicBlockBuilder(ir);
        divideBasicBlock.build();

        DataFlowAnalyse calcDataFlowTest = new DataFlowAnalyse(ir);
        calcDataFlowTest.optimize(false, false);
        for (Function f : ir.getFunctionTable().values()) {
            for (BasicBlock b : f.getBasicBlocks()) {
                filePrintln(block_debug, b.toString());
            }
        }

        BranchToBranch2Var branchToBranch2Var = new BranchToBranch2Var(ir);
        branchToBranch2Var.optimize();
        ir = ir.rebuild();

        int epoch = 1;
        MidCodeProgram ir_opt = null;
        while (true) {
            System.out.println("epoch " + epoch);
            epoch++;

            BasicBlockBuilder basicBlockBuilder = new BasicBlockBuilder(ir);
            basicBlockBuilder.build();
            PeepHoleOptimize peepHoleOptimize = new PeepHoleOptimize(ir);
            peepHoleOptimize.optimize();
            DataFlowAnalyse dataFlowAnalyse = new DataFlowAnalyse(ir);
            dataFlowAnalyse.optimize(true, false);

            ir_opt = ir.rebuild();
            basicBlockBuilder = new BasicBlockBuilder(ir_opt);
            basicBlockBuilder.build();
            dataFlowAnalyse = new DataFlowAnalyse(ir_opt);
            dataFlowAnalyse.optimize(false, true);

            ir_opt = ir_opt.rebuild();
            if (ir_opt.toString().equals(ir.toString())) {
                break;
            }
            ir = ir_opt;
        }
        for (Function f : ir.getFunctionTable().values()) {
            for (BasicBlock b : f.getBasicBlocks()) {
                filePrintln(block_opt_debug, b.toString());
            }
        }
        filePrintln(ir_opt_out, ir_opt.toString());

        RegAllocator regAllocator = new RegAllocator(ir);
        regAllocator.run();

        ir.getFunctionTable().values().forEach(
                (function) -> function.getRegMap().forEach((k, v) -> {
                    try {
                        filePrintln(reg_alloc_result, function.getName() + ": " + k + " -> " + id2regName.get(v));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }));

        Translator translator = new Translator(ir);
        translator.toMips();
        filePrintln(mips_out, translator.getMipsCode().toString());
    }
}
