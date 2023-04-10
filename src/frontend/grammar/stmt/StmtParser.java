package frontend.grammar.stmt;

import frontend.error.Error;
import frontend.error.ErrorTable;
import frontend.grammar.decl.DeclParser;
import frontend.grammar.expr.ExprParser;
import frontend.grammar.expr.linked_expr.Cond;
import frontend.grammar.expr.linked_expr.Exp;
import frontend.grammar.expr.unary_expr.LVal;
import frontend.grammar.expr.unary_expr.PrimaryExp;
import frontend.grammar.expr.unary_expr.PrimaryExpBase;
import frontend.grammar.expr.unary_expr.UnaryExpBase;
import frontend.lexical.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import static frontend.lexical.Token.TokenType.*;

public class StmtParser {
    private final ListIterator<Token> tokenListIterator;
    private final ErrorTable errorTable;

    public StmtParser(ListIterator<Token> tokenListIterator, ErrorTable errorTable) {
        this.tokenListIterator = tokenListIterator;
        this.errorTable = errorTable;
    }

    public StmtParser(List<Token> tokenList, ErrorTable errorTable) {
        this(tokenList.listIterator(), errorTable);
    }

    public void addMissingRightParenthesisError() {
        tokenListIterator.previous();
        Token lastToken = tokenListIterator.next();
        errorTable.add(new Error(Error.ErrorType.MISSING_RIGHT_PARENT, lastToken.getLinenumber()));
    }

    public void addMissingSemicolonError() {
        tokenListIterator.previous();
        Token lastToken = tokenListIterator.next();
        errorTable.add(new Error(Error.ErrorType.MISSING_SEMICOLON, lastToken.getLinenumber()));
    }

    private Token pickSpecifiedTokenOrNull(ListIterator<Token> iterator, Token.TokenType specifiedTokenType) {
        if (iterator.hasNext()) {
            Token token = iterator.next();
            if (token.getTokenType() == specifiedTokenType) {
                return token;
            } else {
                iterator.previous();
            }
        }
        return null;
    }

    private Token pickSpecifiedNextToken(ListIterator<Token> iterator, Token.TokenType specifiedTokenType) {
        if (iterator.hasNext()) {
            Token token = iterator.next();
            if (token.getTokenType() == specifiedTokenType) {
                return token;
            } else {
                iterator.previous();
                throw new RuntimeException("Expected " + specifiedTokenType + " but got " + token.getTokenType());
            }
        }
        throw new RuntimeException("Expected " + specifiedTokenType + " but got nothing");
    }

    private Token pickNotNullNextToken(ListIterator<Token> iterator) {
        if (iterator.hasNext()) {
            return iterator.next();
        }
        throw new RuntimeException("Expected a token but got nothing");
    }

    private boolean isExpFirst(Token token) {
        return token.getTokenType().equals(IDENFR)
                || token.getTokenType().equals(INTCON)
                || token.getTokenType().equals(LPARENT)
                || token.getTokenType().equals(PLUS)
                || token.getTokenType().equals(MINU)
                || token.getTokenType().equals(NOT);
    }

    /*
        Stmt → LVal '=' Exp ';' // 每种类型的语句都要覆盖
             | [Exp] ';' //有无Exp两种情况
             | Block
             | 'if' '(' Cond ')' Stmt [ 'else' Stmt ] // 1.有else 2.无else
             | 'while' '(' Cond ')' Stmt
             | 'break' ';' | 'continue' ';'
             | 'return' [Exp] ';' // 1.有Exp 2.无Exp
             | LVal '=' 'getint''('')'';'
             | 'printf''('FormatString{','Exp}')'';' // 1.有Exp 2.无Exp
     */
    public Stmt parseStmt() {
        Token token = tokenListIterator.next();
        switch (token.getTokenType()) {
            case IFTK:
                return parseIfStmt(token);
            case WHILETK:
                return parseWhileStmt(token);
            case BREAKTK:
                return parseBreakStmt(token);
            case CONTINUETK:
                return parseContinueStmt(token);
            case RETURNTK:
                return parseReturnStmt(token);
            case PRINTFTK:
                return parsePrintStmt(token);
            case LBRACE:
                return parseBlockStmt(token);
            case SEMICN:
                return new EmptyStmt(token);
            default:
                tokenListIterator.previous();
                return parseOthers();
        }
    }

    // 可能缺少右小括号')'
    public IfStmt parseIfStmt(Token ifToken) {
        Token leftParen = pickSpecifiedNextToken(tokenListIterator, LPARENT);
        Cond cond = new ExprParser(tokenListIterator, errorTable).parseCond();
        Token rightParen = pickSpecifiedTokenOrNull(tokenListIterator, RPARENT);
        if (rightParen == null) {
            Token lastToken = tokenListIterator.previous(); tokenListIterator.next();
            errorTable.add(new Error(Error.ErrorType.MISSING_RIGHT_PARENT, lastToken.getLinenumber()));
        }
        Stmt stmt = parseStmt();
        if (tokenListIterator.hasNext()) {
            Token elseToken = tokenListIterator.next();
            if (elseToken.getTokenType() == ELSETK) {
                Stmt elseStmt = parseStmt();
                return new IfStmt(ifToken, leftParen, cond, rightParen, stmt, elseToken, elseStmt);
            } else {
                tokenListIterator.previous();
                return new IfStmt(ifToken, leftParen, cond, rightParen, stmt);
            }
        } else {
            return new IfStmt(ifToken, leftParen, cond, rightParen, stmt);
        }
    }

    public BreakStmt parseBreakStmt(Token breakToken) {
        Token token = pickSpecifiedTokenOrNull(tokenListIterator, SEMICN);
        if (token == null) addMissingSemicolonError();
        return new BreakStmt(breakToken, token);
    }

    public ContinueStmt parseContinueStmt(Token continueToken) {
        Token token = pickSpecifiedTokenOrNull(tokenListIterator, SEMICN);
        if (token == null) addMissingSemicolonError();
        return new ContinueStmt(continueToken, token);
    }

    // 可能缺少分号';'
    public ReturnStmt parseReturnStmt(Token returnToken) {
        if (tokenListIterator.hasNext()) {
            Token token = tokenListIterator.next();
            if (token.getTokenType() == SEMICN) {
                return new ReturnStmt(returnToken, token);
            } else if (isExpFirst(token)) {
                tokenListIterator.previous();
                Exp returnExp = new ExprParser(tokenListIterator, errorTable).parseExp();
                Token semicolon = pickSpecifiedTokenOrNull(tokenListIterator, SEMICN);
                if (semicolon == null) addMissingSemicolonError();
                return new ReturnStmt(returnToken, returnExp, semicolon);
            } else {
                tokenListIterator.previous();
                errorTable.add(new Error(Error.ErrorType.MISSING_SEMICOLON, returnToken.getLinenumber()));
                return new ReturnStmt(returnToken, null);
            }
        } else {
            throw new RuntimeException("Expected ';' but got EOF");
        }
    }

    // 可能缺少右小括号')', 缺少分号';', 有非法格式字符串
    public PrintStmt parsePrintStmt(Token printfToken) {
        Token leftParen = pickSpecifiedNextToken(tokenListIterator, LPARENT);
        Token formatString = tokenListIterator.next();
        if (formatString.getTokenType() == ERR) {
            errorTable.add(new Error(Error.ErrorType.ILLEGAL_FORMAT_STRING, formatString.getLinenumber()));
        } else if (formatString.getTokenType() != STRCON) {
            throw new RuntimeException("Expected String but got " + formatString.getTokenType());
        }
        Token token = tokenListIterator.next();
        if (token.getTokenType() == COMMA) {
            List<Token> commas = new ArrayList<>();
            List<Exp> exps = new ArrayList<>();
            while(token.getTokenType() == COMMA) {
                commas.add(token);
                exps.add(new ExprParser(tokenListIterator, errorTable).parseExp());
                token = tokenListIterator.next();
            }
            if (token.getTokenType() == RPARENT) {  // 可能缺少分号
                Token semicolon = pickSpecifiedTokenOrNull(tokenListIterator, SEMICN);
                if (semicolon == null) errorTable.add(new Error(Error.ErrorType.MISSING_SEMICOLON, token.getLinenumber()));
                return new PrintStmt(printfToken, leftParen, formatString, commas, exps, token, semicolon);
            } else if (token.getTokenType() == SEMICN) {    // 可能缺少右小括号
                tokenListIterator.previous();
                Token lastToken = tokenListIterator.previous();
                errorTable.add(new Error(Error.ErrorType.MISSING_RIGHT_PARENT, lastToken.getLinenumber()));
                tokenListIterator.next(); tokenListIterator.next();
                return new PrintStmt(printfToken, leftParen, formatString, commas, exps, null, token);
            } else {    // 可能缺少分号, 可能缺少右小括号
                Token lastToken = tokenListIterator.previous(); tokenListIterator.next();
                errorTable.add(new Error(Error.ErrorType.MISSING_RIGHT_PARENT, lastToken.getLinenumber()));
                errorTable.add(new Error(Error.ErrorType.MISSING_SEMICOLON, lastToken.getLinenumber()));
                return new PrintStmt(printfToken, leftParen, formatString, commas, exps, null, null);
            }
        } else if (token.getTokenType() == RPARENT) {   // 可能缺少分号
            Token semicolon = pickSpecifiedTokenOrNull(tokenListIterator, SEMICN);
            if (semicolon == null) addMissingSemicolonError();
            return new PrintStmt(printfToken, leftParen, formatString, token, semicolon);
        } else {    // 可能缺少分号, 可能缺少右小括号
            Token semicolon = pickSpecifiedTokenOrNull(tokenListIterator, SEMICN);
            errorTable.add(new Error(Error.ErrorType.MISSING_RIGHT_PARENT, formatString.getLinenumber()));
            if (semicolon == null) errorTable.add(new Error(Error.ErrorType.MISSING_SEMICOLON, formatString.getLinenumber()));
            return new PrintStmt(printfToken, leftParen, formatString, null, semicolon);
        }
    }

    // 可能缺少右小括号')'
    public WhileStmt parseWhileStmt(Token whileToken) {
        Token leftParen = pickSpecifiedNextToken(tokenListIterator, LPARENT);
        Cond cond = new ExprParser(tokenListIterator, errorTable).parseCond();
        Token rightParen = pickSpecifiedTokenOrNull(tokenListIterator, RPARENT);
        if (rightParen == null) addMissingRightParenthesisError();
        Stmt stmt = parseStmt();
        return new WhileStmt(whileToken, leftParen, cond, rightParen, stmt);
    }

    public BlockStmt parseBlockStmt(Token leftBrace) {
        return new BlockStmt(parseBlock(leftBrace));
    }

    public Block parseBlock(Token leftBrace) {
        List<BlockItem> blockItems = new ArrayList<>();
        while (tokenListIterator.hasNext()) {
            Token token = tokenListIterator.next();
            if (token.getTokenType() == RBRACE) {
                return new Block(leftBrace, blockItems, token);
            } else {
                tokenListIterator.previous();
                blockItems.add(parseBlockItem());
            }
        }
        throw new RuntimeException("Expected '}' but got EOF");
    }

    public BlockItem parseBlockItem() {
        if (tokenListIterator.hasNext()) {
            Token token = tokenListIterator.next();
            tokenListIterator.previous();
            if (token.getTokenType() == CONSTTK) {
                return new DeclParser(tokenListIterator, errorTable).parseDecl();
            } else if (token.getTokenType() == INTTK) {
                return new DeclParser(tokenListIterator, errorTable).parseDecl();
            } else {
                return parseStmt();
            }
        } else {
            throw new RuntimeException("Expected BlockItem but got EOF");
        }
    }

    // 最复杂的一种情况: AssignStmt & ExpStmt & InputStmt
    // LVal '=' Exp ';'
    // [Exp] ';'
    // LVal '=' 'getint''('')'';'
    // 三者均以一个Exp开头，那么我们先尝试判断读到的第一个token是否属于Exp的FIRST集
    // - 如果不是的话，直接报错
    // - 如果是的话，那么我们拆开包装，看这个Exp是不是一个LVal
    //      - 如果是的话，那么我们再看下一个token是不是等号
    //          - 这样就分开了AssignStmt&InputStmt和ExpStmt
    //          - 再根据等号后面的token来判断是AssignStmt还是InputStmt
    //      - 如果不是的话，那么我们再看下一个token是不是分号
    //          - 如果是，直接返回一个ExpStmt
    //          - 如果不是，报错
    public Stmt parseOthers() {
        Token token = tokenListIterator.next();
        if (isExpFirst(token)) {    // 判断读到的第一个token是否属于Exp的FIRST集
            tokenListIterator.previous();
            // 拆开包装，看这个Exp是不是一个LVal
            Exp exp = new ExprParser(tokenListIterator, errorTable).parseExp();
            UnaryExpBase expFirst = exp.getAddExp().getFirst().getFirst().getUnaryExpBase();
            LVal lVal = null;
            if (expFirst instanceof PrimaryExp) {
                PrimaryExpBase primaryExpBase = ((PrimaryExp) expFirst).getPrimaryExpBase();
                if (primaryExpBase instanceof LVal) {
                    lVal = (LVal) primaryExpBase;
                }
            }
            if (lVal == null) {     // 如果不是一个LVal
                Token semicolon = pickSpecifiedTokenOrNull(tokenListIterator, SEMICN);
                if (semicolon == null) addMissingSemicolonError();
                return new ExpStmt(exp, semicolon);
            } else {        // // 如果是一个LVal
                // 再看下一个token是不是等号
                Token assignToken = tokenListIterator.next();
                if (assignToken.getTokenType() != ASSIGN) {
                    if (assignToken.getTokenType() == SELFADD) {
                        Token semicolon = pickSpecifiedTokenOrNull(tokenListIterator, SEMICN);
                        if (semicolon == null) addMissingSemicolonError();
                        return new SelfAddStmt(lVal, assignToken, semicolon);
                    } else if (assignToken.getTokenType() == SELFSUB) {
                        Token semicolon = pickSpecifiedTokenOrNull(tokenListIterator, SEMICN);
                        if (semicolon == null) addMissingSemicolonError();
                        return new SelfSubStmt(lVal, assignToken, semicolon);
                    }
                    tokenListIterator.previous();
                    Token semicolon = pickSpecifiedTokenOrNull(tokenListIterator, SEMICN);
                    if (semicolon == null) addMissingSemicolonError();
                    return new ExpStmt(exp, semicolon);
                }
                // 再根据等号后面的token来判断是AssignStmt还是InputStmt
                Token getintToken = tokenListIterator.next();
                if (getintToken.getTokenType() == GETINTTK) {       // 找到InputStmt
                    Token leftParen = pickSpecifiedNextToken(tokenListIterator, LPARENT);
                    Token rightParen = pickSpecifiedTokenOrNull(tokenListIterator, RPARENT);
                    if (rightParen == null) addMissingRightParenthesisError();
                    Token semicolon = pickSpecifiedTokenOrNull(tokenListIterator, SEMICN);
                    if (semicolon == null) addMissingSemicolonError();
                    return new InputStmt(lVal, assignToken, getintToken, leftParen, rightParen, semicolon);
                } else {        // 找到AssignStmt
                    tokenListIterator.previous();
                    Exp exp2 = new ExprParser(tokenListIterator, errorTable).parseExp();
                    Token semicolon = pickSpecifiedTokenOrNull(tokenListIterator, SEMICN);
                    if (semicolon == null) addMissingSemicolonError();
                    return new AssignStmt(lVal, assignToken, exp2, semicolon);
                }
            }
        } else {
            throw new RuntimeException("Expected Stmt but got " + token.getTokenType());
        }
    }
}
