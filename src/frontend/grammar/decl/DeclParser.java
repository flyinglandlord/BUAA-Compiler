package frontend.grammar.decl;

import frontend.error.Error;
import frontend.error.ErrorTable;
import frontend.grammar.decl.const_decl.*;
import frontend.grammar.decl.var_decl.*;
import frontend.grammar.expr.ExprParser;
import frontend.grammar.expr.linked_expr.ConstExp;
import frontend.grammar.expr.linked_expr.Exp;
import frontend.lexical.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class DeclParser {
    private final ListIterator<Token> tokenListIterator;
    private final ErrorTable errorTable;

    public DeclParser(ListIterator<Token> tokenListIterator, ErrorTable errorTable) {
        this.tokenListIterator = tokenListIterator;
        this.errorTable = errorTable;
    }

    public DeclParser(List<Token> tokenList, ErrorTable errorTable) {
        this(tokenList.listIterator(), errorTable);
    }

    // <Decl> := <ConstDecl> | <VarDecl>
    public Decl parseDecl() {
        if (tokenListIterator.hasNext()) {
            Token token = tokenListIterator.next();
            if (token.getTokenType() == Token.TokenType.CONSTTK) {
                return parseConstDecl(token);
            } else if (token.getTokenType() == Token.TokenType.INTTK) {
                return parseVarDecl(token);
            }
        }
        throw new RuntimeException("Expected <ConstDecl> or <VarDecl>");
    }

    // <VarDecl> := <BType> <VarDef> { ',' <VarDef> } ';'
    // 可能缺分号;
    public VarDecl parseVarDecl(Token intToken) {
        Token BType = intToken;
        if (tokenListIterator.hasNext()) {
            VarDef first = parseVarDef();
            List<VarDef> rest = new ArrayList<>();
            List<Token> commas = new ArrayList<>();
            while (tokenListIterator.hasNext()) {
                Token token = tokenListIterator.next();
                //System.out.println(token);
                if (token.getTokenType() == Token.TokenType.COMMA) {
                    commas.add(token);
                    rest.add(parseVarDef());
                } else if (token.getTokenType() == Token.TokenType.SEMICN) {
                    return new VarDecl(BType, first, rest, commas, token);
                } else {
                    tokenListIterator.previous();
                    Token lastToken = tokenListIterator.previous(); tokenListIterator.next();
                    // System.out.println("<parse>" + lastToken.getContent());
                    errorTable.add(new Error(Error.ErrorType.MISSING_SEMICOLON, lastToken.getLinenumber()));
                    return new VarDecl(BType, first, rest, commas, null);
                }
            }
        }
        throw new RuntimeException("Expected <VarDef>");
    }

    // <VarDef> := Ident { '[' <ConstExp> ']' } | Ident { '[' <ConstExp> ']' } '=' <InitVal>
    public VarDef parseVarDef() {
        if (tokenListIterator.hasNext()) {
            Token ident = tokenListIterator.next();
            //System.out.println(ident);
            if (ident.getTokenType() == Token.TokenType.IDENFR) {
                List<ArraySubscriptDef> arraySubscriptDefs = new ArrayList<>();
                while(tokenListIterator.hasNext()) {
                    Token token = tokenListIterator.next();
                    if (token.getTokenType() == Token.TokenType.LBRACK) {
                        arraySubscriptDefs.add(parseArrayDef(token));
                    } else {
                        tokenListIterator.previous();
                        break;
                    }
                }
                Token assignToken = tokenListIterator.next();
                if (assignToken.getTokenType() == Token.TokenType.ASSIGN) {
                    InitVal initVal = parseInitVal();
                    return new VarDef(ident, arraySubscriptDefs, assignToken, initVal);
                } else {
                    tokenListIterator.previous();
                    return new VarDef(ident, arraySubscriptDefs);
                }
            }
        }
        return null;
    }

    // <InitVal> := <Exp> | '{' [ <InitVal> { ',' <InitVal> } ] '}'
    // '{' -> ArrayInitVar
    // 否则是 Exp
    public InitVal parseInitVal() {
        if (tokenListIterator.hasNext()) {
            Token leftBrace = tokenListIterator.next();
            if (leftBrace.getTokenType() == Token.TokenType.LBRACE) {   // 解析 ArrayInitVar
                List<InitVal> rest = new java.util.ArrayList<>();
                List<Token> commas = new java.util.ArrayList<>();
                Token rightBrace = tokenListIterator.next();
                if (rightBrace.getTokenType() == Token.TokenType.RBRACE) {  // 防止空数组 {} 的情况
                    return new ArrayInitVar(leftBrace, rightBrace, null, rest, commas);
                } else tokenListIterator.previous();
                InitVal first = parseInitVal();
                while (tokenListIterator.hasNext()) {
                    Token token = tokenListIterator.next();
                    if (token.getTokenType() == Token.TokenType.COMMA) {
                        commas.add(token);
                        rest.add(parseInitVal());
                    } else if (token.getTokenType() == Token.TokenType.RBRACE) {
                        return new ArrayInitVar(leftBrace, token, first, rest, commas);
                    }
                }
            } else {    // 直接解析一个Exp
                tokenListIterator.previous();
                ExprParser exprParser = new ExprParser(tokenListIterator, errorTable);
                Exp exp = exprParser.parseExp();
                return new ExpInitVar(exp);
            }
        }
        throw new RuntimeException("Expected <ConstExp> or '{' [ <ConstInitVal> { ',' <ConstInitVal> } ] '}'");
    }

    // <ConstDecl> := 'const' <BType> <ConstDef> { ',' <ConstDef> } ';'
    // 可能缺分号
    public ConstDecl parseConstDecl(Token constToken) {
        if (tokenListIterator.hasNext()) {
            Token BType = tokenListIterator.next();
            if (BType.getTokenType() != Token.TokenType.INTTK) {
                throw new RuntimeException("Expected 'int'");
            }
            // 读取若干个 ConstDef
            ConstDef first = parseConstDef();
            List<ConstDef> rest = new java.util.ArrayList<>();
            List<Token> commas = new java.util.ArrayList<>();
            while (tokenListIterator.hasNext()) {
                Token token = tokenListIterator.next();
                //System.out.println(token);
                if (token.getTokenType() == Token.TokenType.COMMA) {
                    commas.add(token);
                    rest.add(parseConstDef());
                } else if (token.getTokenType() == Token.TokenType.SEMICN) {
                    return new ConstDecl(constToken, BType, first, rest, commas, token);
                } else {
                    tokenListIterator.previous();
                    Token lastToken = tokenListIterator.previous(); tokenListIterator.next();
                    // System.out.println("<parse>" + lastToken.getContent());
                    errorTable.add(new Error(Error.ErrorType.MISSING_SEMICOLON, lastToken.getLinenumber()));
                    return new ConstDecl(constToken, BType, first, rest, commas, null);
                }
            }
        }
        throw new RuntimeException("Expected 'const' <BType> <ConstDef> { ',' <ConstDef> } ';'");
    }

    // <ConstDef> := Ident { '[' <ConstExp> ']' } '=' <ConstInitVal>
    public ConstDef parseConstDef() {
        if (tokenListIterator.hasNext()) {
            Token ident = tokenListIterator.next();
            if (ident.getTokenType() == Token.TokenType.IDENFR) {
                List<ArraySubscriptDef> arraySubscriptDefs = new java.util.ArrayList<>();
                while (tokenListIterator.hasNext()) {
                    Token token = tokenListIterator.next();
                    if (token.getTokenType() == Token.TokenType.LBRACK) {
                        arraySubscriptDefs.add(parseArrayDef(token));
                    } else if (token.getTokenType() == Token.TokenType.ASSIGN) {
                        ConstInitVal constInitVal = parseConstInitVal();
                        return new ConstDef(ident, arraySubscriptDefs, token, constInitVal);
                    } else {
                        tokenListIterator.previous();
                        break;
                    }
                }
            }
        }
        return null;
    }

    // <ConstInitVal>  := <ConstExp> | '{' [ <ConstInitVal> { ',' <ConstInitVal> } ] '}'
    public ConstInitVal parseConstInitVal() {
        if (tokenListIterator.hasNext()) {      // 解析 ConstArrayInitVar
            Token leftBrace = tokenListIterator.next();
            if (leftBrace.getTokenType() == Token.TokenType.LBRACE) {
                List<ConstInitVal> rest = new java.util.ArrayList<>();
                List<Token> commas = new java.util.ArrayList<>();
                Token rightBrace = tokenListIterator.next();
                if (rightBrace.getTokenType() == Token.TokenType.RBRACE) {
                    return new ConstArrayInitVar(leftBrace, rightBrace, null, rest, commas);
                } else tokenListIterator.previous();
                ConstInitVal first = parseConstInitVal();
                while (tokenListIterator.hasNext()) {
                    Token token = tokenListIterator.next();
                    if (token.getTokenType() == Token.TokenType.COMMA) {
                        commas.add(token);
                        rest.add(parseConstInitVal());
                    } else if (token.getTokenType() == Token.TokenType.RBRACE) {
                        return new ConstArrayInitVar(leftBrace, token, first, rest, commas);
                    }
                }
            } else {    // 解析 ConstExpInitVar
                tokenListIterator.previous();
                ConstExp constExp = new ExprParser(tokenListIterator, errorTable).parseConstExp();
                return new ConstExpInitVar(constExp);
            }
        }
        throw new RuntimeException("Expected <ConstExp> or '{' [ <ConstInitVal> { ',' <ConstInitVal> } ] '}'");
    }

    // ArraySubscriptDef -> '[' <ConstExp> ']'
    // 可能出现缺少]
    public ArraySubscriptDef parseArrayDef(Token leftBracket) {
        ConstExp constExp = new ExprParser(tokenListIterator, errorTable).parseConstExp();
        Token rightBracket = tokenListIterator.next();
        if (rightBracket.getTokenType() != Token.TokenType.RBRACK) {
            Token lastToken = tokenListIterator.previous();
            errorTable.add(new Error(Error.ErrorType.MISSING_RIGHT_BRACKET, lastToken.getLinenumber()));
            return new ArraySubscriptDef(leftBracket, constExp, null);
        }
        return new ArraySubscriptDef(leftBracket, constExp, rightBracket);
    }
}
