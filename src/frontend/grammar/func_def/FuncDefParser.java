package frontend.grammar.func_def;

import frontend.error.Error;
import frontend.error.ErrorTable;
import frontend.grammar.expr.ExprParser;
import frontend.grammar.expr.linked_expr.ConstExp;
import frontend.grammar.stmt.Block;
import frontend.grammar.stmt.StmtParser;
import frontend.lexical.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class FuncDefParser {
    private final ListIterator<Token> tokenListIterator;
    private final ErrorTable errorTable;

    public FuncDefParser(ListIterator<Token> tokenListIterator, ErrorTable errorTable) {
        this.tokenListIterator = tokenListIterator;
        this.errorTable = errorTable;
    }

    public FuncDefParser(List<Token> tokenList, ErrorTable errorTable) {
        this(tokenList.listIterator(), errorTable);
    }

    public void addMissingRightParenthesisError() {
        tokenListIterator.previous();
        Token lastToken = tokenListIterator.next();
        errorTable.add(new Error(Error.ErrorType.MISSING_RIGHT_PARENT, lastToken.getLinenumber()));
    }

    public void addMissingRightBracketError() {
        tokenListIterator.previous();
        Token lastToken = tokenListIterator.next();
        errorTable.add(new Error(Error.ErrorType.MISSING_RIGHT_BRACKET, lastToken.getLinenumber()));
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

    // FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
    // 可能缺少右小括号')'
    public FuncDef parseFuncDef() {
        Token funcType = tokenListIterator.next();
        if (funcType.getTokenType() == Token.TokenType.INTTK ||
                funcType.getTokenType() == Token.TokenType.VOIDTK) {
            Token ident = pickSpecifiedNextToken(tokenListIterator, Token.TokenType.IDENFR);
            Token leftParen = pickSpecifiedNextToken(tokenListIterator, Token.TokenType.LPARENT);
            Token rightParen = pickNotNullNextToken(tokenListIterator);
            if (rightParen.getTokenType() == Token.TokenType.RPARENT) {
                Token leftBrace = pickSpecifiedNextToken(tokenListIterator, Token.TokenType.LBRACE);
                Block block = new StmtParser(tokenListIterator, errorTable).parseBlock(leftBrace);
                return new FuncDef(new FuncType(funcType), ident, leftParen, rightParen, block);
            } else if (rightParen.getTokenType() == Token.TokenType.LBRACE) {
                tokenListIterator.previous();
                errorTable.add(new Error(Error.ErrorType.MISSING_RIGHT_PARENT, leftParen.getLinenumber()));
                Token leftBrace = pickSpecifiedNextToken(tokenListIterator, Token.TokenType.LBRACE);
                Block block = new StmtParser(tokenListIterator, errorTable).parseBlock(leftBrace);
                return new FuncDef(new FuncType(funcType), ident, leftParen, null, block);
            } else {
                tokenListIterator.previous();
                FuncFParams funcFParams = parseFuncFParams();
                rightParen = pickSpecifiedTokenOrNull(tokenListIterator, Token.TokenType.RPARENT);
                if (rightParen == null) addMissingRightParenthesisError();
                Token leftBrace = pickSpecifiedNextToken(tokenListIterator, Token.TokenType.LBRACE);
                Block block = new StmtParser(tokenListIterator, errorTable).parseBlock(leftBrace);
                return new FuncDef(new FuncType(funcType), ident, leftParen, funcFParams, rightParen, block);
            }
        } else {
            throw new RuntimeException("Expected 'int' or 'void'");
        }
    }

    // FuncFParams → FuncFParam { ',' FuncFParam }
    public FuncFParams parseFuncFParams() {
        FuncFParam first = parseFuncFParam();
        List<Token> commas = new ArrayList<>();
        List<FuncFParam> rest = new ArrayList<>();
        while (tokenListIterator.hasNext()) {
            Token token = tokenListIterator.next();
            if (token.getTokenType() == Token.TokenType.COMMA) {
                commas.add(token);
                rest.add(parseFuncFParam());
            } else {
                tokenListIterator.previous();
                break;
            }
        }
        return new FuncFParams(first, commas, rest);
    }

    // 可能会缺少右中括号’]’
    public FuncFParam parseFuncFParam() {
        Token bType = pickSpecifiedNextToken(tokenListIterator, Token.TokenType.INTTK);
        Token ident = pickSpecifiedNextToken(tokenListIterator, Token.TokenType.IDENFR);
        List<FuncFParamArrayDef> subscripts = new ArrayList<>();
        Token token = null;
        while(tokenListIterator.hasNext()) {
            token = tokenListIterator.next();
            if (token.getTokenType() == Token.TokenType.LBRACK) {
                Token rightBracket = pickNotNullNextToken(tokenListIterator);
                ConstExp constExp = null;
                if (rightBracket.getTokenType() == Token.TokenType.RPARENT ||
                        rightBracket.getTokenType() == Token.TokenType.COMMA) {
                    tokenListIterator.previous();
                    addMissingRightBracketError();
                    subscripts.add(new FuncFParamArrayDef(token, null));
                } else if (rightBracket.getTokenType() != Token.TokenType.RBRACK) {
                    tokenListIterator.previous();
                    constExp = new ExprParser(tokenListIterator, errorTable).parseConstExp();
                    rightBracket = pickSpecifiedTokenOrNull(tokenListIterator, Token.TokenType.RBRACK);
                    if (rightBracket == null) addMissingRightBracketError();
                    subscripts.add(new FuncFParamArrayDef(token, constExp, rightBracket));
                } else {
                    subscripts.add(new FuncFParamArrayDef(token, rightBracket));
                }
            } else {
                tokenListIterator.previous();
                break;
            }
        }
        return new FuncFParam(bType, ident, subscripts);
    }

    // MainFuncDef → 'int' 'main' '(' ')' Block
    public MainFuncDef parseMainFuncDef() {
        Token intToken = pickSpecifiedNextToken(tokenListIterator, Token.TokenType.INTTK);
        Token mainToken = pickSpecifiedNextToken(tokenListIterator, Token.TokenType.MAINTK);
        Token leftParen = pickSpecifiedNextToken(tokenListIterator, Token.TokenType.LPARENT);
        Token rightParen = pickSpecifiedNextToken(tokenListIterator, Token.TokenType.RPARENT);
        Token leftBrace = tokenListIterator.next();
        if (leftBrace.getTokenType() == Token.TokenType.LBRACE) {
            Block block = new StmtParser(tokenListIterator, errorTable).parseBlock(leftBrace);
            return new MainFuncDef(intToken, mainToken, leftParen, rightParen, block);
        } else {
            throw new RuntimeException("Expected '{'");
        }
    }
}
