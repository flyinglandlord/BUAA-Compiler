package frontend.build_middle;

import backend.instructions.other.La;
import frontend.error.Error;
import frontend.error.ErrorTable;
import frontend.grammar.Node;
import frontend.grammar.comp_unit.CompUnit;
import frontend.grammar.decl.Decl;
import frontend.grammar.decl.ArraySubscriptDef;
import frontend.grammar.decl.const_decl.*;
import frontend.grammar.decl.var_decl.*;
import frontend.grammar.expr.linked_expr.*;
import frontend.grammar.expr.unary_expr.*;
import frontend.grammar.expr.unary_expr.Number;
import frontend.grammar.func_def.*;
import frontend.grammar.stmt.*;
import frontend.lexical.Token;
import middle.middle_code.MidCodeList;
import middle.middle_code.MidCodeProgram;
import middle.middle_code.element.*;
import middle.middle_code.operand.ArrayItem;
import middle.middle_code.operand.ArrayPointer;
import middle.middle_code.operand.Immediate;
import middle.middle_code.operand.Operand;
import middle.symbol.*;

import java.util.*;

public class Visitor {
    private int blockDepth = 0;
    private final Map<Integer, Integer> blockLocation = new HashMap<>();
    private final Stack<Stmt> loopStmt = new Stack<>();
    private final Stack<Label> loopContinueLabel = new Stack<>();
    private final Stack<Label> loopBreakLabel = new Stack<>();
    private final ErrorTable errorTable;
    private final MidCodeProgram midCodeProgram = new MidCodeProgram();
    private MidCodeList currentMidCodeList = midCodeProgram.getGlobalVarDeclCode();
    private SymbolTable currentSymbolTable = midCodeProgram.getGlobalSymbolTable();
    private Function currentFunction = null;

    public Visitor(ErrorTable errorTable) {
        this.errorTable = errorTable;
    }

    public MidCodeProgram getMidCodeProgram() {
        return midCodeProgram;
    }

    public ErrorTable getErrorTable() {
        return errorTable;
    }

    public SymbolTable getCurrentSymbolTable() {
        return currentSymbolTable;
    }

    public Function getCurrentFunction() {
        return currentFunction;
    }

    public void analyseCompUnit(CompUnit unit) throws ConstExpException {
        for (Decl decl : unit.getDeclList()) {
            analyseDecl(decl);
        }
        for (FuncDef funcDef : unit.getFuncDefList()) {
            analyseFunc(funcDef);
        }
        blockLocation.put(1, blockLocation.getOrDefault(1, 0) + 1);
        SymbolTable childSymbolTable = new SymbolTable(1, blockLocation.get(1), currentSymbolTable);
        currentSymbolTable.addChild(childSymbolTable);
        currentSymbolTable = childSymbolTable;
        Function mainMeta = new Function("main", childSymbolTable,
                unit.getMainFuncDef().getBlock(), Function.ReturnType.INT);
        midCodeProgram.getFunctionTable().put("main", mainMeta);
        currentFunction = mainMeta;
        MainFuncDef main = unit.getMainFuncDef();
        currentMidCodeList = new MidCodeList();
        currentMidCodeList.addMidCode(new Label("main", false));
        mainMeta.setBody(currentMidCodeList);
        analyseFuncBody(main.getBlock(), mainMeta);
        /*for (MidCode i : currentMidCodeList.getMidCodeList()) {
            System.out.println(i);
        }*/
    }

    private void analyseFuncBody(Block funcBody, Function meta) throws ConstExpException {
        currentSymbolTable = meta.getParamTable();
        //System.out.println(currentSymbolTable.getDepth() + ", " + currentSymbolTable.getLocation());
        for (Symbol symbol : meta.getParamList()) {
            currentMidCodeList.addMidCode(new ParamDef(symbol));
        }
        analyseBlock(funcBody);
        currentSymbolTable = currentSymbolTable.getParent();
        currentFunction = null;
        boolean returnFlag = false;
        Iterator<BlockItem> iterator = funcBody.getBlockItems().listIterator();
        while (iterator.hasNext()) {
            BlockItem blockItem = iterator.next();
            if (!iterator.hasNext()) {      // 找到函数的最后一条语句
                if (blockItem instanceof Stmt &&
                        ((Stmt) blockItem) instanceof ReturnStmt) {
                    returnFlag = true;
                }
            }
        }
        if (!(returnFlag || meta.getReturnType().equals(Function.ReturnType.VOID))) {
            errorTable.add(new Error(Error.ErrorType.MISSING_RETURN,
                    funcBody.getRightBrace().getLinenumber()));
        }
    }

    public void analyseFunc(FuncDef func) throws ConstExpException {
        // 缺右括号
        if (func.getRightParen() == null) {
            errorTable.add(new Error(Error.ErrorType.MISSING_RIGHT_PARENT, func.getIdent().getLinenumber()));
        }

        Function.ReturnType returnType =
                func.getFuncType().getbType().getTokenType().equals(Token.TokenType.VOIDTK) ?
                        Function.ReturnType.VOID : Function.ReturnType.INT;
        String funcName = func.getIdent().getContent();

        boolean error = false;
        // 与之前定义的函数重名
        if (midCodeProgram.getFunctionTable().containsKey(funcName)) {
            errorTable.add(new Error(Error.ErrorType.REDEFINED_IDENT,
                    func.getIdent().getLinenumber()));
            error = true;
        }
        // 与之前定义的全局变量重名
        if (currentSymbolTable.contains(funcName, false)) {
            errorTable.add(new Error(Error.ErrorType.REDEFINED_IDENT,
                    func.getIdent().getLinenumber()));
            error = true;
        }

        blockLocation.put(1, blockLocation.getOrDefault(1, 0) + 1);
        SymbolTable childSymbolTable = new SymbolTable(1, blockLocation.get(1), currentSymbolTable);
        currentSymbolTable.addChild(childSymbolTable);
        currentSymbolTable = childSymbolTable;
        Function meta = new Function(funcName, childSymbolTable,
                func.getBlock(), returnType);
        if (!error) midCodeProgram.getFunctionTable().put(funcName, meta);
        currentFunction = meta;
        currentMidCodeList = new MidCodeList();
        meta.setBody(currentMidCodeList);

        currentMidCodeList.addMidCode(new Label(funcName, false));
        if (func.getFuncFParams() != null) {
            FuncFParams fParams = func.getFuncFParams();
            FuncFParam first = fParams.getFirst();
            analyseFuncFParam(first, meta);
            for (FuncFParam funcFParam : fParams.getRest()) {
                analyseFuncFParam(funcFParam, meta);
            }
        }
        analyseFuncBody(func.getBlock(), meta);
        /*for (MidCode i : currentMidCodeList.getMidCodeList()) {
            System.out.println(i);
        }*/
    }

    public void analyseFuncFParam(FuncFParam param, Function meta) throws ConstExpException {
        String paramName = param.getIdent().getContent();
        // 与之前定义的参数重名
        if (meta.getParamTable().contains(paramName, false)) {
            errorTable.add(new Error(Error.ErrorType.REDEFINED_IDENT,
                    param.getIdent().getLinenumber()));
            return;
        }
        if (param.getSubscripts() == null) {    // 不是数组
            meta.addParam(new FunctionFormParam(paramName));
        } else {    // 是数组
            List<Integer> shape = new ArrayList<>();
            for (FuncFParamArrayDef subscript : param.getSubscripts()) {
                ConstExp len = subscript.getConstExp();
                int length = 0;
                // TODO: 计算出数组长度
                if (len != null) length = new CalcConstExpr(currentSymbolTable, errorTable).calcExp(len);
                if (length == -1) throw new RuntimeException("Array length must be a constant");
                shape.add(length);
            }
            meta.addParam(new FunctionFormParam(paramName, shape.size(), shape));
            // System.out.println("<analyseFuncFParam>");
            // System.out.println(shape.size());
        }
    }

    public void analyseDecl(Decl decl) throws ConstExpException {
        if (decl instanceof VarDecl) {
            VarDecl varDecl = (VarDecl) decl;
            VarDef first = varDecl.getFirst();
            analyseVarDef(first);
            for (VarDef varDef : varDecl.getRest()) {
                analyseVarDef(varDef);
            }
        } else if (decl instanceof ConstDecl) {
            ConstDecl constDecl = (ConstDecl) decl;
            ConstDef first = constDecl.getFirst();
            analyseConstDef(first);
            for (ConstDef constDef : constDecl.getRest()) {
                analyseConstDef(constDef);
            }
        }
    }

    public void analyseVarDef(VarDef varDef) throws ConstExpException {
        String varName = varDef.getIdent().getContent();
        if (currentSymbolTable.contains(varName, false)) {
            errorTable.add(new Error(Error.ErrorType.REDEFINED_IDENT, varDef.getIdent().getLinenumber()));
            return;
        }
        if (blockDepth == 1 && currentFunction != null &&
                currentFunction.getParamTable().contains(varName, false)) {
            errorTable.add(new Error(Error.ErrorType.REDEFINED_IDENT, varDef.getIdent().getLinenumber()));
            return;
        }
        if (varDef.getGetIntToken() != null) {
            Variable newVar = new Variable(varName, false);
            currentMidCodeList.addMidCode(new DeclareVar(DeclareVar.Type.VAR_DEF, newVar));
            currentSymbolTable.add(newVar);
            currentMidCodeList.addMidCode(new GetInt((Symbol) newVar));
        } else if (varDef.getArrayDefs() == null || varDef.getArrayDefs().isEmpty()) {    // 不是数组定义
            if (varDef.getAssignToken() != null && varDef.getInitVal() != null) {   // 被初始化
                if (currentFunction == null) {      // 如果是全局变量定义，那么初始值一定可以计算出来
                    int initValue = new CalcExpr(currentSymbolTable, errorTable)
                            .calcExp(((ExpInitVar) varDef.getInitVal()).getExp());
                    Variable newVar = new Variable(varName, false, varDef.getInitVal(), initValue);
                    currentSymbolTable.add(newVar);
                    currentMidCodeList.addMidCode(new DeclareVar(DeclareVar.Type.VAR_DEF, newVar,
                            new Immediate(initValue)));
                } else {    // 如果不是全局变量定义，把定义和赋值分开
                    Variable newVar = new Variable(varName, false, varDef.getInitVal());
                    currentSymbolTable.add(newVar);
                    currentMidCodeList.addMidCode(new DeclareVar(DeclareVar.Type.VAR_DEF, newVar));
                    Operand initValue = analyseExpr(((ExpInitVar) varDef.getInitVal()).getExp());
                    currentMidCodeList.addMidCode(new Assign(newVar, initValue));
                }
            } else {    // 未被初始化
                Variable newVar = new Variable(varName, false);
                currentSymbolTable.add(newVar);
                if (currentFunction == null) currentMidCodeList.addMidCode(new DeclareVar(DeclareVar.Type.VAR_DEF, newVar, new Immediate(0)));
                else currentMidCodeList.addMidCode(new DeclareVar(DeclareVar.Type.VAR_DEF, newVar));
            }
        } else if (varDef.getArrayDefs() != null) {    // 是数组定义
            List<Integer> shape = new ArrayList<>();
            int totalSize = 1;
            for (ArraySubscriptDef arrayDef : varDef.getArrayDefs()) {
                ConstExp len = arrayDef.getArraySize();
                int length = new CalcConstExpr(currentSymbolTable, errorTable).calcExp(len);
                shape.add(length);
                totalSize *= length;
            }
            if (varDef.getAssignToken() != null && varDef.getInitVal() != null) {   // 被初始化的数组
                if (currentFunction == null) {              // 如果是全局数组定义，所有初始值应当可以计算出来
                    List<Operand> initValues = new ArrayList<>();
                    List<Integer> calcInitValues = new ArrayList<>();
                    ArrayInitVar arrayInitVar = (ArrayInitVar) varDef.getInitVal();
                    // 一维数组
                    if (shape.size() == 1) {
                        List<ExpInitVar> expInitVars = new ArrayList<>();
                        expInitVars.add((ExpInitVar) arrayInitVar.getFirst());
                        for (InitVal i : arrayInitVar.getRest()) {
                            expInitVars.add((ExpInitVar) i);
                        }
                        for (ExpInitVar expInitVar : expInitVars) {
                            int initValue = new CalcExpr(currentSymbolTable, errorTable)
                                    .calcExp(expInitVar.getExp());
                            initValues.add(new Immediate(initValue));
                            calcInitValues.add(initValue);
                        }
                    } else if (shape.size() == 2) {     // 二维数组
                        List<ArrayInitVar> initVars = new ArrayList<>();
                        initVars.add((ArrayInitVar) arrayInitVar.getFirst());
                        for (InitVal i : arrayInitVar.getRest()) {
                            initVars.add((ArrayInitVar) i);
                        }
                        for (ArrayInitVar initVar : initVars) {
                            int initVal = new CalcExpr(currentSymbolTable, errorTable)
                                    .calcExp(((ExpInitVar)(initVar.getFirst())).getExp());
                            initValues.add(new Immediate(initVal));
                            calcInitValues.add(initVal);
                            for (int i = 0; i < shape.get(1) - 1; i++) {
                                initVal = new CalcExpr(currentSymbolTable, errorTable)
                                        .calcExp(((ExpInitVar)(initVar.getRest().get(i))).getExp());
                                initValues.add(new Immediate(initVal));
                                calcInitValues.add(initVal);
                            }
                        }
                    } else throw new RuntimeException("Array dimension must be 1 or 2");
                    Variable newArr = new Variable(varName, false, varDef.getInitVal(), shape.size(), shape, calcInitValues);
                    currentSymbolTable.add(newArr);
                    currentMidCodeList.addMidCode(new DeclareArray(DeclareArray.Type.VAR_DEF, newArr, totalSize, initValues));
                } else {                                    // 如果不是全局数组定义，把定义和赋值分开
                    Variable newArr = new Variable(varName, false, varDef.getInitVal(), shape.size(), shape);
                    currentSymbolTable.add(newArr);
                    ArrayInitVar arrayInitVar = (ArrayInitVar) varDef.getInitVal();
                    List<Operand> initValues = new ArrayList<>();
                    // 一维数组
                    if (shape.size() == 1) {
                        List<ExpInitVar> expInitVars = new ArrayList<>();
                        if (arrayInitVar.getFirst() != null) expInitVars.add((ExpInitVar) arrayInitVar.getFirst());
                        for (InitVal i : arrayInitVar.getRest()) {
                            expInitVars.add((ExpInitVar) i);
                        }
                        for (ExpInitVar expInitVar : expInitVars) {
                            Operand initValue = analyseExpr(expInitVar.getExp());
                            initValues.add(initValue);
                        }
                    } else if (shape.size() == 2) {     // 二维数组
                        // TODO: 为了方便通过不规范的辅助测试库，考虑括号不足的情况
                        // 主要是这些情况: {x, x, x}, {{x, x}, {}, {x, x}}
                        List<ArrayInitVar> initVars = new ArrayList<>();
                        initVars.add((ArrayInitVar)arrayInitVar.getFirst());
                        for (InitVal i : arrayInitVar.getRest()) {
                            initVars.add((ArrayInitVar) i);
                        }
                        for (ArrayInitVar initVar : initVars) {
                            Operand val = analyseExpr(((ExpInitVar)(initVar.getFirst())).getExp());
                            initValues.add(val);
                            for (int i = 0; i < initVar.getRest().size(); i++) {
                                val = analyseExpr(((ExpInitVar)(initVar.getRest().get(i))).getExp());
                                initValues.add(val);
                            }
                            for (int i = initVar.getRest().size() + 1; i < shape.get(1); i++) {
                                if (currentFunction == null) initValues.add(new Immediate(0));
                                else initValues.add(null);
                            }
                        }
                    } else throw new RuntimeException("Array dimension must be 1 or 2");
                    currentMidCodeList.addMidCode(new DeclareArray(DeclareArray.Type.VAR_DEF,
                            newArr, totalSize));
                    for (int i = 0; i < initValues.size(); i++) {
                        if (Objects.nonNull(initValues.get(i))) {
                            currentMidCodeList.addMidCode(new LoadSave(LoadSave.Op.STORE, newArr, new Immediate(i), initValues.get(i)));
                        }
                    }
                }
            } else {
                Variable newArr = new Variable(varName, false, shape.size(), shape);
                currentSymbolTable.add(newArr);
                if (currentFunction == null) {          // 如果是全局变量，全部赋值为0
                    ArrayList<Operand> initVal = new ArrayList<>();
                    for (int i = 0; i < totalSize; i++) {
                        initVal.add(new Immediate(0));
                    }
                    currentMidCodeList.addMidCode(new DeclareArray(DeclareArray.Type.VAR_DEF,
                            newArr, totalSize, initVal));
                }
                else currentMidCodeList.addMidCode(new DeclareArray(DeclareArray.Type.VAR_DEF, newArr, totalSize));
            }
        }
    }

    public void analyseConstDef(ConstDef constDef) throws ConstExpException {
        String constName = constDef.getIdent().getContent();
        if (currentSymbolTable.contains(constName, false)) {
            errorTable.add(new Error(Error.ErrorType.REDEFINED_IDENT, constDef.getIdent().getLinenumber()));
            return;
        }
        if (blockDepth == 1 && currentFunction != null &&
                currentFunction.getParamTable().contains(constName, false)) {
            errorTable.add(new Error(Error.ErrorType.REDEFINED_IDENT, constDef.getIdent().getLinenumber()));
            return;
        }
        if (constDef.getArrayDefs() == null || constDef.getArrayDefs().isEmpty()) {    // 不是数组定义
            if (constDef.getAssignToken() != null && constDef.getConstInitVal() != null) {   // 被初始化
                int calcConstVal = new CalcConstExpr(currentSymbolTable, errorTable)
                        .calcExp(((ConstExpInitVar) constDef.getConstInitVal()).getConstExp());
                Variable newVar = new Variable(constName, true, constDef.getConstInitVal(), calcConstVal);
                currentSymbolTable.add(newVar);
                currentMidCodeList.addMidCode(new DeclareVar(DeclareVar.Type.CONST_DEF, newVar, new Immediate(calcConstVal)));
            } else {    // 未被初始化
                throw new RuntimeException("ConstDef must be initialized");
            }
        } else if (constDef.getArrayDefs() != null) {    // 是数组定义
            List<Integer> shape = new ArrayList<>();
            int totalSize = 1;
            for (ArraySubscriptDef arrayDef : constDef.getArrayDefs()) {
                ConstExp len = arrayDef.getArraySize();
                int length = 0;
                length = new CalcConstExpr(currentSymbolTable, errorTable).calcExp(len);
                // if (length == -1) throw new RuntimeException("Array length must be a constant");
                // TODO: 计算ConstExp的值
                shape.add(length);
                totalSize *= length;
            }
            if (constDef.getAssignToken() != null && constDef.getConstInitVal() != null) {   // 被初始化的数组
                List<Operand> initValues = new ArrayList<>();
                List<Integer> calcInitValues = new ArrayList<>();
                ConstArrayInitVar arrayInitVar = (ConstArrayInitVar) constDef.getConstInitVal();
                // 一维数组
                if (shape.size() == 1) {
                    List<ConstExpInitVar> expInitVars = new ArrayList<>();
                    expInitVars.add((ConstExpInitVar) arrayInitVar.getFirst());
                    for (ConstInitVal i : arrayInitVar.getRest()) {
                        expInitVars.add((ConstExpInitVar) i);
                    }
                    for (ConstExpInitVar expInitVar : expInitVars) {
                        int initValue = new CalcConstExpr(currentSymbolTable, errorTable)
                                .calcExp(expInitVar.getConstExp());
                        initValues.add(new Immediate(initValue));
                        calcInitValues.add(initValue);
                    }
                } else if (shape.size() == 2) {     // 二维数组
                    List<ConstArrayInitVar> initVars = new ArrayList<>();
                    initVars.add((ConstArrayInitVar) arrayInitVar.getFirst());
                    for (ConstInitVal i : arrayInitVar.getRest()) {
                        initVars.add((ConstArrayInitVar) i);
                    }
                    for (ConstArrayInitVar initVar : initVars) {
                        int initVal = new CalcConstExpr(currentSymbolTable, errorTable)
                                .calcExp(((ConstExpInitVar)(initVar.getFirst())).getConstExp());
                        initValues.add(new Immediate(initVal));
                        calcInitValues.add(initVal);
                        for (int i = 0; i < shape.get(1) - 1; i++) {
                            initVal = new CalcConstExpr(currentSymbolTable, errorTable)
                                    .calcExp(((ConstExpInitVar)(initVar.getRest().get(i))).getConstExp());
                            initValues.add(new Immediate(initVal));
                            calcInitValues.add(initVal);
                        }
                    }
                } else throw new RuntimeException("Array dimension must be 1 or 2");
                Variable newArr = new Variable(constName, true, constDef.getConstInitVal(), shape.size(), shape, calcInitValues);
                currentSymbolTable.add(newArr);
                currentMidCodeList.addMidCode(new DeclareArray(DeclareArray.Type.CONST_DEF, newArr, totalSize, initValues));
            } else {
                throw new RuntimeException("ConstDef must be initialized");
            }
        }
    }

    public void analyseStmt(Stmt stmt) throws ConstExpException {
        if (stmt instanceof IfStmt) {
            analyseIfStmt((IfStmt) stmt);
        } else if (stmt instanceof WhileStmt) {
            analyseWhileStmt((WhileStmt) stmt);
        } else if (stmt instanceof ReturnStmt) {
            analyseReturnStmt((ReturnStmt) stmt);
        } else if (stmt instanceof BreakStmt) {
            analyseBreakStmt((BreakStmt) stmt);
        } else if (stmt instanceof ContinueStmt) {
            analyseContinueStmt((ContinueStmt) stmt);
        } else if (stmt instanceof ExpStmt) {
            analyseExpr(((ExpStmt) stmt).getExp());
        } else if (stmt instanceof InputStmt) {
            analyseInputStmt((InputStmt) stmt);
        } else if (stmt instanceof PrintStmt) {
            analysePrintStmt((PrintStmt) stmt);
        } else if (stmt instanceof AssignStmt) {
            analyseAssignStmt((AssignStmt) stmt);
        } else if (stmt instanceof BlockStmt) {
            analyseBlock(((BlockStmt) stmt).getBlock());
        } else if (stmt instanceof EmptyStmt) {
            // do nothing
        } else if (stmt instanceof SelfAddStmt) {
            analyseSelfAddStmt((SelfAddStmt) stmt);
        } else if (stmt instanceof SelfSubStmt) {
            analyseSelfSubStmt((SelfSubStmt) stmt);
        } else {
            throw new RuntimeException("Unknown Stmt type");
        }
    }

    public void analyseSelfAddStmt(SelfAddStmt stmt) {
        Operand dst = analysePrimaryExprBase(stmt.getlVal(), true);
        if (dst instanceof ArrayItem) {
            Symbol tmp = Symbol.tempSymbol();
            currentMidCodeList.addMidCode(new LoadSave(LoadSave.Op.LOAD, ((ArrayItem) dst).getBase(),
                    ((ArrayItem) dst).getOffset(), tmp));
            currentMidCodeList.addMidCode(new BinaryOp(BinaryOp.Op.ADD, new Immediate(1), tmp, tmp));
            currentMidCodeList.addMidCode(new LoadSave(LoadSave.Op.STORE, ((ArrayItem) dst).getBase(),
                    ((ArrayItem) dst).getOffset(), tmp));
        } else if (dst instanceof Symbol) {
            Symbol tmp = Symbol.tempSymbol();
            currentMidCodeList.addMidCode(new BinaryOp(BinaryOp.Op.ADD, new Immediate(1), (Symbol) dst, tmp));
            currentMidCodeList.addMidCode(new Assign((Symbol) dst, tmp));
        }
    }

    public void analyseSelfSubStmt(SelfSubStmt stmt) {
        Operand dst = analysePrimaryExprBase(stmt.getlVal(), true);
        if (dst instanceof ArrayItem) {
            Symbol tmp = Symbol.tempSymbol();
            currentMidCodeList.addMidCode(new LoadSave(LoadSave.Op.LOAD, ((ArrayItem) dst).getBase(),
                    ((ArrayItem) dst).getOffset(), tmp));
            currentMidCodeList.addMidCode(new BinaryOp(BinaryOp.Op.SUB, new Immediate(1), tmp, tmp));
            currentMidCodeList.addMidCode(new LoadSave(LoadSave.Op.STORE, ((ArrayItem) dst).getBase(),
                    ((ArrayItem) dst).getOffset(), tmp));
        } else if (dst instanceof Symbol) {
            Symbol tmp = Symbol.tempSymbol();
            currentMidCodeList.addMidCode(new BinaryOp(BinaryOp.Op.SUB, new Immediate(1), (Symbol) dst, tmp));
            currentMidCodeList.addMidCode(new Assign((Symbol) dst, tmp));
        }
    }

    public void analyseBlock(Block stmt) throws ConstExpException {
        blockDepth++;
        if (blockDepth > 1) {
            blockLocation.put(blockDepth, blockLocation.getOrDefault(blockDepth, 0) + 1);
            SymbolTable childSymbolTable = new SymbolTable(blockDepth, blockLocation.get(blockDepth), currentSymbolTable);
            currentSymbolTable.addChild(childSymbolTable);
            currentSymbolTable = childSymbolTable;
        }
        //currentMidCodeList.addMidCode(new BlockIdent(BlockIdent.Type.BEGIN, blockDepth, blockLocation.get(blockDepth)));
        for (BlockItem item : stmt.getBlockItems()) {
            if (item instanceof Stmt) {
                analyseStmt((Stmt) item);
            } else if (item instanceof Decl) {
                analyseDecl((Decl) item);
            } else {
                throw new AssertionError("BlockItem wrong refType!");
            }
        }
        //currentMidCodeList.addMidCode(new BlockIdent(BlockIdent.Type.END, blockDepth, blockLocation.get(blockDepth)));
        if (blockDepth > 1) {
            currentSymbolTable = currentSymbolTable.getParent();
        }
        blockDepth--;
    }

    public void analyseWhileStmt(WhileStmt stmt) throws ConstExpException {
        Label condLabel = new Label("COND_", true);
        Label endCondLabel = new Label("END_COND_", true);
        Label bodyLabel = new Label("LOOP_BODY_", true);
        Label whileLabel = new Label("WHILE_", true);
        Label endLabel = new Label("END_WHILE_", true);
        loopStmt.add(stmt);
        loopBreakLabel.add(endLabel);
        loopContinueLabel.add(whileLabel);

        currentMidCodeList.addMidCode(new Jump(condLabel));
        currentMidCodeList.addMidCode(condLabel);

        Operand first_cond = analyseCond(stmt.getCond(), endCondLabel, endLabel);
        currentMidCodeList.addMidCode(new Branch(Branch.Type.EQ, endLabel, first_cond));

        currentMidCodeList.addMidCode(endCondLabel);
        currentMidCodeList.addMidCode(new Jump(bodyLabel));
        currentMidCodeList.addMidCode(bodyLabel);

        analyseStmt(stmt.getStmt());

        currentMidCodeList.addMidCode(new Jump(whileLabel));
        currentMidCodeList.addMidCode(whileLabel);

        Operand cond = analyseCond(stmt.getCond(), bodyLabel, endLabel);
        currentMidCodeList.addMidCode(new Branch(Branch.Type.NE, bodyLabel, cond));

        currentMidCodeList.addMidCode(endLabel);

        loopBreakLabel.pop();
        loopContinueLabel.pop();
        loopStmt.pop();
    }

    public void analyseIfStmt(IfStmt stmt) throws ConstExpException {
        Label endLabel = new Label("END_IF_", true);
        Label thenLabel = new Label("IF_THEN_", true);
        if (stmt.getElseToken() != null) {
            Label elseLabel = new Label("IF_ELSE_", true);
            Operand cond = analyseCond(stmt.getCond(), thenLabel, elseLabel);
            currentMidCodeList.addMidCode(new Branch(Branch.Type.EQ, elseLabel, cond));

            currentMidCodeList.addMidCode(thenLabel);
            analyseStmt(stmt.getStmt());
            currentMidCodeList.addMidCode(new Jump(endLabel));

            currentMidCodeList.addMidCode(elseLabel);
            analyseStmt(stmt.getElseStmt());
        } else {
            Operand cond = analyseCond(stmt.getCond(), thenLabel, endLabel);
            currentMidCodeList.addMidCode(new Branch(Branch.Type.EQ, endLabel, cond));

            currentMidCodeList.addMidCode(thenLabel);
            analyseStmt(stmt.getStmt());
        }
        currentMidCodeList.addMidCode(new Jump(endLabel));
        currentMidCodeList.addMidCode(endLabel);
    }

    public void analyseReturnStmt(ReturnStmt stmt) {
        if (currentFunction == null) {
            throw new RuntimeException("ReturnStmt must be in a function");
        }
        if (currentFunction.getReturnType() == Function.ReturnType.INT) {
            if (stmt.getExp() == null) {
                throw new RuntimeException("ReturnStmt must have a return value");
            }
            Operand ret = analyseExpr(stmt.getExp());
            currentMidCodeList.addMidCode(new Return(ret));
        } else if (currentFunction.getReturnType() == Function.ReturnType.VOID) {
            if (stmt.getExp() != null) {
                errorTable.add(new Error(Error.ErrorType.VOID_FUNC_RETURN_VALUE,
                        stmt.getReturnToken().getLinenumber()));
            }
            currentMidCodeList.addMidCode(new Return());
        }
    }

    public void analyseBreakStmt(BreakStmt stmt) {
        if (loopStmt.isEmpty()) {
            errorTable.add(new Error(Error.ErrorType.CONTROL_OUTSIDE_LOOP,
                    stmt.getBreakToken().getLinenumber()));
        } else currentMidCodeList.addMidCode(new Jump(loopBreakLabel.peek()));
    }

    public void analyseContinueStmt(ContinueStmt stmt) {
        if (loopStmt.isEmpty()) {
            errorTable.add(new Error(Error.ErrorType.CONTROL_OUTSIDE_LOOP,
                    stmt.getContinueToken().getLinenumber()));
        } else currentMidCodeList.addMidCode(new Jump(loopContinueLabel.peek()));
    }

    public void analyseAssignStmt(AssignStmt stmt) {
        Operand dst = analysePrimaryExprBase(stmt.getlVal(), true);
        Operand src = analyseExpr(stmt.getExp());
        if (dst instanceof ArrayItem) {
            currentMidCodeList.addMidCode(new LoadSave(LoadSave.Op.STORE, ((ArrayItem) dst).getBase(),
                    ((ArrayItem) dst).getOffset(), src));
        } else if (dst instanceof Symbol) {
            currentMidCodeList.addMidCode(new Assign((Symbol) dst, src));
        }
    }

    public void analyseInputStmt(InputStmt stmt) {
        Operand dst = analysePrimaryExprBase(stmt.getlVal(), true);
        if (dst instanceof ArrayItem) {
            Symbol tmp = Symbol.tempSymbol();
            currentSymbolTable.addTempVar(tmp);
            currentMidCodeList.addMidCode(new GetInt(tmp));
            currentMidCodeList.addMidCode(new LoadSave(LoadSave.Op.STORE, ((ArrayItem) dst).getBase(),
                    ((ArrayItem) dst).getOffset(), tmp));
        } else if (dst instanceof Symbol) {
            currentMidCodeList.addMidCode(new GetInt((Symbol) dst));
        }
    }

    private int checkFormatString(String format) {
        int count = 0;
        for (int i = 1; i < format.length()-1; i++) {
            char c = format.charAt(i);
            if (c != 32 && c != 33 && !(c >= 40 && c <= 126)) {
                if (c == '%') {
                    if (i < format.length() - 1 && format.charAt(i + 1) == 'd') {
                        count = count + 1;
                        continue;
                    } else {
                        return -1;
                    }
                }
                return -1;
            }
            if (c == 92 && (i >= format.length() - 1 || format.charAt(i + 1) != 'n')) {
                return -1;
            }
        }
        return count;
    }

    public void analysePrintStmt(PrintStmt stmt) {
        String formatString = stmt.getFormatString().getContent();
        int count = checkFormatString(formatString);
        if (count < 0) {
            errorTable.add(new Error(Error.ErrorType.ILLEGAL_FORMAT_STRING, stmt.getFormatString().getLinenumber()));
            return;
        }
        if (count != stmt.getExp().size()) {
            errorTable.add(new Error(Error.ErrorType.MISMATCH_PARAM_PRINTF, stmt.getPrintfToken().getLinenumber()));
            return;
        }
        List<Operand> operands = new ArrayList<>();
        for (Exp exp : stmt.getExp()) {
            Operand ret = analyseExpr(exp);
            if (ret == null) throw new RuntimeException("Expr in PrintStmt cannot be void");
            operands.add(ret);
        }
        Iterator<Operand> iterator = operands.iterator();
        /*if (formatString.substring(1, formatString.length() - 1).startsWith("%d")) {
            currentMidCodeList.addMidCode(new Print(iterator.next()));
        }*/
        String[] splitString = formatString.substring(1, formatString.length() - 1).split("%d");
        //System.out.print("analysePrintStmt:");
        //for (String s : splitString) {
        //    System.out.print("\'" + s + "\'" + " ");
        //}
        //System.out.println();
        for (String i : splitString) {
            if (!i.isEmpty()) {
                Symbol stringSymbol = Symbol.stringSymbol();
                midCodeProgram.getGlobalStringTable().put(stringSymbol, i);
                currentMidCodeList.addMidCode(new Print(stringSymbol));
            }
            if (iterator.hasNext()) currentMidCodeList.addMidCode(new Print(iterator.next()));
        }
        while (iterator.hasNext()) currentMidCodeList.addMidCode(new Print(iterator.next()));
    }

    public Operand analysePrimaryExprBase(PrimaryExpBase base, boolean isLVal) {
        if (base instanceof SubExpr) {
            if (isLVal) throw new RuntimeException("SubExpr can't be lVal");
            SubExpr sub = (SubExpr) base;
            return analyseExpr(sub.getExp());
        } else if (base instanceof LVal) {
            // 符号表相关错误(变量未定义等)
            LVal lVal = (LVal) base;
            if (!currentSymbolTable.contains(lVal.getIdent().getContent(), true)) {
                errorTable.add(new Error(Error.ErrorType.UNDEFINED_IDENT, lVal.getIdent().getLinenumber()));
                return new Immediate(0);
            }
            Symbol symbol = currentSymbolTable.get(lVal.getIdent().getContent(), true);
            // 尝试修改一个真正的左值报错
            if (isLVal && symbol.isConst()) {
                errorTable.add(new Error(Error.ErrorType.MODIFY_CONST, lVal.getIdent().getLinenumber()));
                return new Immediate(0);
            }

            List<Operand> index = new ArrayList<>();
            for (Subscript subscript : lVal.getSubscriptList()) {
                index.add(analyseExpr(subscript.getIndex()));
            }
            Operand offset = new Immediate(0);
            for (int i = index.size() - 1; i >= 0; i--) {
                // offset += arrayIndexes[i] * baseOffset;
                int suffix_prod = 1;
                for (int j = symbol.getShape().size() - 1; j > i; j--) {
                    suffix_prod *= symbol.getShape().get(j);
                }
                Operand prod = null;
                if (index.get(i) instanceof Immediate) {    // 如果两个立即数，直接返回乘积
                    prod = new Immediate(((Immediate) index.get(i)).getValue() * suffix_prod);
                } else if (suffix_prod == 1) {              // 如果是乘上一个1，直接优化掉
                    prod = index.get(i);
                } else {                                    // 否则得生成一句计算指令
                    prod = Symbol.tempSymbol();
                    currentSymbolTable.addTempVar((Symbol) prod);
                    Operand offsetBase = new Immediate(suffix_prod);
                    currentMidCodeList.addMidCode(new BinaryOp(BinaryOp.Op.MUL, index.get(i), offsetBase, (Symbol) prod));
                }
                if (offset instanceof Immediate && prod instanceof Immediate) {
                    offset = new Immediate(((Immediate) offset).getValue() + ((Immediate) prod).getValue());
                } else if (offset instanceof Immediate && ((Immediate) offset).getValue() == 0) {
                    offset = prod;
                } else if (prod instanceof Immediate && ((Immediate) prod).getValue() == 0) {
                    // do nothing
                } else {
                    Symbol sum = Symbol.tempSymbol();
                    currentSymbolTable.addTempVar(sum);
                    currentMidCodeList.addMidCode(new BinaryOp(BinaryOp.Op.ADD, offset, prod, sum));
                    offset = sum;
                }
            }
            if (!symbol.isArray()) {    // 如果不是数组，直接返回符号表中符号
                return symbol;
            } else {                    // 如果是数组，把符号表项包装一层
                if (isLVal) {
                    // TODO: 这里应该返回ArrayItem类型
                    return new ArrayItem(symbol, offset);
                } else {
                    if (lVal.getSubscriptList().size() < symbol.getShape().size()) {
                        return new ArrayPointer(symbol, lVal.getSubscriptList().size(), offset);
                    } else {
                        Symbol value = Symbol.tempSymbol();
                        currentSymbolTable.addTempVar(value);
                        currentMidCodeList.addMidCode(new LoadSave(LoadSave.Op.LOAD, symbol, offset, value));
                        return value;
                    }
                }
            }
        } else if (base instanceof frontend.grammar.expr.unary_expr.Number) {
            if (isLVal) throw new RuntimeException("Number can't be lVal");
            return new Immediate(((Number) base).getIntConst().getValue());
        } else {
            throw new RuntimeException("BasePrimaryExp refType error!");
        }
    }

    public Operand analyseExpr(Exp exp) {
        try {
            int ret = new CalcConstExpr(currentSymbolTable, errorTable).calcExp(exp);
            return new Immediate(ret);
        } catch (ConstExpException ignore) {
            // pass
        }
        return analyseLinkedExpr(exp.getAddExp());
    }

    private Operand analyseBinaryOrUnaryExp(Node node) {
        if (node instanceof LinkedExpr) {
            return analyseLinkedExpr((LinkedExpr<?>) node);
        } else if (node instanceof UnaryExp) {
            return analyseUnaryExp((UnaryExp) node);
        } else {
            throw new RuntimeException("BinaryOrUnaryExp refType error!");
        }
    }

    public Operand analyseLinkedExpr(LinkedExpr<?> exp) {
        Node first = exp.getFirst();
        Operand ret = analyseBinaryOrUnaryExp(first);
        if (ret == null) {
            return null;
        }
        Iterator<Token> iterOp = exp.getLinkOperator().iterator();
        Iterator<?> iterSrc = exp.getRest().iterator();
        while (iterOp.hasNext() && iterSrc.hasNext()) {
            Token op = iterOp.next();
            Node src = (Node) iterSrc.next();
            Operand subResult = analyseBinaryOrUnaryExp(src);
            if (subResult == null) {
                return null;
            }
            if (ret instanceof Immediate && subResult instanceof Immediate) {
                // 两个立即数运算可以立即返回立即数结果，无需生成指令
                ret = new Immediate(BinaryOpCalculate(op, ((Immediate) ret).getValue(), ((Immediate) subResult).getValue()));
            } else if (((ret instanceof Immediate && ((Immediate) ret).getValue() == 0) ||
                    (subResult instanceof Immediate && ((Immediate) subResult).getValue() == 0)) &&
                    (op.getTokenType() == Token.TokenType.PLUS || op.getTokenType() == Token.TokenType.MINU)) {
                // 一个符号加0或者减0，也不用生成运算指令
                if (op.getTokenType() == Token.TokenType.MINU) {
                    if (ret instanceof Immediate) {
                        Symbol neg = Symbol.tempSymbol();
                        currentSymbolTable.addTempVar(neg);
                        currentMidCodeList.addMidCode(new UnaryOp(UnaryOp.Op.NEG, subResult, neg));
                        ret = neg;
                    }
                } else if (op.getTokenType() == Token.TokenType.PLUS) {
                    if (ret instanceof Immediate) {
                        ret = subResult;
                    }
                }
            } else if (((ret instanceof Immediate && ((Immediate) ret).getValue() == 0) ||
                    (subResult instanceof Immediate && ((Immediate) subResult).getValue() == 0)) &&
                    (op.getTokenType() == Token.TokenType.MULT)) {
                // 一个符号乘0，也不用生成运算指令
                ret = new Immediate(0);
            } else {
                // 正常情况两个操作数，生成一句运算指令
                Symbol tmp = Symbol.tempSymbol();
                currentSymbolTable.addTempVar(tmp);
                currentMidCodeList.addMidCode(new BinaryOp(tokenToBinaryOp(op), ret, subResult, tmp));
                ret = tmp;
            }
        }
        return ret;
    }

    public Operand analyseUnaryExp(UnaryExp exp) {
        Operand result = null;
        try {
            result = new Immediate(new CalcConstExpr(currentSymbolTable, errorTable).calcUnaryExp(exp));
            //System.out.println(result);
            return result;
        } catch (ConstExpException ignore) {
            // 这意味着不是常量表达式，需要生成中间代码
        }
        UnaryExpBase base = exp.getUnaryExpBase();
        if (base instanceof FuncCall) {
            result = analyseFuncCall((FuncCall) base);
        } else if (base instanceof PrimaryExp) {
            PrimaryExp primaryExp = (PrimaryExp) base;
            result = analysePrimaryExprBase(primaryExp.getPrimaryExpBase(), false);
        }
        for (Token op : exp.getOperators()) {
            Symbol tmp = Symbol.tempSymbol();
            currentSymbolTable.addTempVar(tmp);
            currentMidCodeList.addMidCode(new UnaryOp(tokenToUnaryOp(op), result, tmp));
            result = tmp;
        }
        return result;
    }

    public Operand analyseFuncCall(FuncCall funcCall) {
        String funcName = funcCall.getFuncName().getContent();
        if (!midCodeProgram.getFunctionTable().containsKey(funcName)) {
            errorTable.add(new Error(Error.ErrorType.UNDEFINED_IDENT, funcCall.getFuncName().getLinenumber()));
            return new Immediate(0);
        }
        Function function = midCodeProgram.getFunctionTable().get(funcName);
        List<Operand> realParams = new ArrayList<>();
        List<FunctionFormParam> formArgs = function.getParamList();
        if (funcCall.hasParams()) {
            List<Exp> funcRParams = funcCall.getFuncRParamsList().getAllParams();
            for (Exp i : funcRParams) {
                Operand r = analyseExpr(i);
                realParams.add(r);
            }
        }
        boolean error = false;
        if (realParams.size() != formArgs.size()) {
            errorTable.add(new Error(Error.ErrorType.MISMATCH_PARAM_NUM, funcCall.getFuncName().getLinenumber()));
            error = true;
        } else {
            Iterator<Operand> iterParam = realParams.listIterator();
            Iterator<FunctionFormParam> iterArg = formArgs.listIterator();
            while (iterParam.hasNext() && iterArg.hasNext()) {
                Operand param = iterParam.next();
                FunctionFormParam arg = iterArg.next();
                if (Objects.isNull(param)) {
                    errorTable.add(new Error(Error.ErrorType.MISMATCH_PARAM_TYPE, funcCall.getFuncName().getLinenumber()));
                    error = true;
                    break;
                } else if (param instanceof Immediate) {
                    if (arg.isArray()) {
                        errorTable.add(new Error(Error.ErrorType.MISMATCH_PARAM_TYPE, funcCall.getFuncName().getLinenumber()));
                        error = true;
                        break;
                    }
                } else if (param instanceof Variable || param instanceof ArrayItem) {
                    if (arg.isArray()) {
                        errorTable.add(new Error(Error.ErrorType.MISMATCH_PARAM_TYPE, funcCall.getFuncName().getLinenumber()));
                        error = true;
                        break;
                    }
                } else if (param instanceof ArrayPointer) {
                    if (!arg.isArray()) {
                        errorTable.add(new Error(Error.ErrorType.MISMATCH_PARAM_TYPE, funcCall.getFuncName().getLinenumber()));
                        error = true;
                        break;
                    } else {
                        ArrayPointer pointer = (ArrayPointer) param;
                        if (arg.getDimension() != pointer.getBase().getShape().size() - pointer.getDimension()) {
                            errorTable.add(new Error(Error.ErrorType.MISMATCH_PARAM_TYPE, funcCall.getFuncName().getLinenumber()));
                            error = true;
                            break;
                        }
                        for (int i = pointer.getDimension(); i < pointer.getBase().getShape().size(); i++) {
                            if (!Objects.equals(pointer.getBase().getShape().get(i), arg.getShape().get(i - pointer.getDimension())) &&
                                    arg.getShape().get(i - pointer.getDimension()) != 0)  {
                                errorTable.add(new Error(Error.ErrorType.MISMATCH_PARAM_TYPE, funcCall.getFuncName().getLinenumber()));
                                error = true;
                                break;
                            }
                        }
                    }
                }
            }
        }
        currentMidCodeList.addMidCode(new FuncCallIdent(FuncCallIdent.Type.BEGIN, function));
        for (Operand r: realParams) {
            if (r instanceof ArrayPointer) {
                currentMidCodeList.addMidCode(new PushParam(PushParam.Type.PUSH_ADDR, r));
            } else {
                currentMidCodeList.addMidCode(new PushParam(PushParam.Type.PUSH, r));
            }
        }
        if (function.getReturnType() == Function.ReturnType.VOID) {
            if (!error) {
                currentMidCodeList.addMidCode(new Call(function, realParams));
                currentMidCodeList.addMidCode(new FuncCallIdent(FuncCallIdent.Type.END, function));
            }
            return null;
        } else {
            if (!error) {
                Symbol tmp = Symbol.tempSymbol();
                currentSymbolTable.addTempVar(tmp);
                currentMidCodeList.addMidCode(new Call(function, realParams, tmp));
                currentMidCodeList.addMidCode(new FuncCallIdent(FuncCallIdent.Type.END, function));
                return tmp;
            } else {
                return new Immediate(0);
            }
        }
    }

    public Operand analyseCond(Cond cond, Label trueLabel, Label falseLabel) {
        return analyseLOrExp(cond.getLOrExp(), trueLabel, falseLabel);
    }

    // 短路求值! 前一项如果为 True 就不用算后面的项了
    public Operand analyseLOrExp(LOrExp exp, Label trueLabel, Label falseLabel) {
        List<LAndExp> lAndExps = new ArrayList<>();
        lAndExps.add(exp.getFirst()); lAndExps.addAll(exp.getRest());
        List<Label> endLabels = new ArrayList<>();
        for (int i = 0; i < lAndExps.size() - 1; i++) {
            Label end = new Label("END_LOR_EXP_", true);
            endLabels.add(end);
        }
        Operand and = null;
        endLabels.add(falseLabel);
        for (int i = 0; i < lAndExps.size(); i++) {
            and = analyseLAndExp(lAndExps.get(i), trueLabel, endLabels.get(i));
            if (i != lAndExps.size() - 1) {
                currentMidCodeList.addMidCode(new Branch(Branch.Type.NE, trueLabel, and));
                currentMidCodeList.addMidCode(endLabels.get(i));
            }
        }
        return and;
    }

    public Operand analyseLAndExp(LAndExp exp, Label trueLabel, Label falseLabel) {
        List<EqExp> eqExps = new ArrayList<>();
        eqExps.add(exp.getFirst()); eqExps.addAll(exp.getRest());
        Operand item = null;
        for (int i = 0; i < eqExps.size(); i++) {
            item = analyseLinkedExpr(eqExps.get(i));
            if (i != eqExps.size() - 1) {
                currentMidCodeList.addMidCode(new Branch(Branch.Type.EQ, falseLabel, item));
            }
        }
        return item;
    }

    private BinaryOp.Op tokenToBinaryOp(Token token) {
        switch (token.getTokenType()) {
            case PLUS: return BinaryOp.Op.ADD;
            case MINU: return BinaryOp.Op.SUB;
            case MULT: return BinaryOp.Op.MUL;
            case DIV: return BinaryOp.Op.DIV;
            case MOD: return BinaryOp.Op.MOD;
            case GEQ: return BinaryOp.Op.GE;
            case GRE: return BinaryOp.Op.GT;
            case LEQ: return BinaryOp.Op.LE;
            case LSS: return BinaryOp.Op.LT;
            case EQL: return BinaryOp.Op.EQ;
            case NEQ: return BinaryOp.Op.NE;
            case BITAND: return BinaryOp.Op.AND;
            default: throw new RuntimeException("Unexpected token type: " + token.getTokenType());
        }
    }

    private int BinaryOpCalculate(Token op, int a, int b) {
        switch (Objects.requireNonNull(tokenToBinaryOp(op))) {
            case ADD: return a + b;
            case SUB: return a - b;
            case MUL: return a * b;
            case DIV: return a / b;
            case MOD: return a % b;
            case GE: return (a >= b) ? 1 : 0;
            case GT: return (a > b) ? 1 : 0;
            case LE: return (a <= b) ? 1 : 0;
            case LT: return (a < b) ? 1 : 0;
            case EQ: return (a == b) ? 1 : 0;
            case NE: return (a != b) ? 1 : 0;
            default: return 0;
        }
    }

    private UnaryOp.Op tokenToUnaryOp(Token token) {
        switch (token.getTokenType()) {
            case PLUS: return UnaryOp.Op.MOV;
            case MINU: return UnaryOp.Op.NEG;
            case NOT: return UnaryOp.Op.NOT;
            default: return null;
        }
    }
}
