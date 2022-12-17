package frontend.grammar.comp_unit;

import frontend.error.ErrorTable;
import frontend.grammar.decl.Decl;
import frontend.grammar.decl.DeclParser;
import frontend.grammar.func_def.FuncDef;
import frontend.grammar.func_def.FuncDefParser;
import frontend.grammar.func_def.MainFuncDef;
import frontend.lexical.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class CompUnitParser {
    private final ListIterator<Token> tokenListIterator;
    private final ErrorTable errorTable;

    public CompUnitParser(ListIterator<Token> tokenListIterator, ErrorTable errorTable) {
        this.tokenListIterator = tokenListIterator;
        this.errorTable = errorTable;
    }

    public CompUnitParser(List<Token> tokenList, ErrorTable errorTable) {
        this(tokenList.listIterator(), errorTable);
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

    // CompUnit → {Decl} {FuncDef} MainFuncDef
    public CompUnit parseCompUnit() {
        List<Decl> declList = parseDecl();
        List<FuncDef> funcDefList = parseFuncDef();
        MainFuncDef mainFuncDef = new FuncDefParser(tokenListIterator, errorTable).parseMainFuncDef();
        return new CompUnit(declList, funcDefList, mainFuncDef);
    }

    // 如何判断是函数声明FuncDef还是变量声明Decl?
    // 根据文法，变量声明一定在函数声明前面
    // Decl有以下几种:
    //      ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';'
    //      VarDecl → BType VarDef { ',' VarDef } ';'
    //          VarDef → Ident { '[' ConstExp ']' }
    // 如果读到 const + int 那么是一个常量声明
    // 如果读到 int + identifier 需要继续判断
    //      如果下一个是左括号 ( 则是函数声明，这是后退三个返回，peek失败
    //      如果下一个不是左括号，就认为是变量声明
    // 否则就后退两个，peek失败，不再是变量声明了
    public List<Decl> parseDecl() {
        Token token1 = pickNotNullNextToken(tokenListIterator);
        Token token2 = pickNotNullNextToken(tokenListIterator);
        List<Decl> declList = new ArrayList<>();
        while (tokenListIterator.hasNext()) {
            //System.out.println(token1);
            //System.out.println(token2);
            if (token1.getTokenType().equals(Token.TokenType.CONSTTK) &&
                    token2.getTokenType().equals(Token.TokenType.INTTK)) {
                tokenListIterator.previous();
                declList.add(new DeclParser(tokenListIterator, errorTable).parseConstDecl(token1));
            } else if (token1.getTokenType().equals(Token.TokenType.INTTK) &&
                    token2.getTokenType().equals(Token.TokenType.IDENFR)) {
                Token token3 = pickNotNullNextToken(tokenListIterator);
                if (token3.getTokenType() == Token.TokenType.LPARENT) {
                    tokenListIterator.previous();
                    break;
                }
                tokenListIterator.previous(); tokenListIterator.previous(); tokenListIterator.previous();
                declList.add(new DeclParser(tokenListIterator, errorTable).parseDecl());
            } else {
                break;
            }
            token1 = pickNotNullNextToken(tokenListIterator);
            token2 = pickNotNullNextToken(tokenListIterator);
        }
        tokenListIterator.previous(); tokenListIterator.previous();
        return declList;
    }

    // 同样，根据文法，函数声明一定在主函数声明前面，直接判断有没有 main (MAINTK) 即可
    public List<FuncDef> parseFuncDef() {
        Token token1 = pickNotNullNextToken(tokenListIterator);
        Token token2 = pickNotNullNextToken(tokenListIterator);
        Token token3 = pickNotNullNextToken(tokenListIterator);
        List<FuncDef> funcDefList = new ArrayList<>();
        while (tokenListIterator.hasNext()) {
            if ((token1.getTokenType().equals(Token.TokenType.INTTK) ||
                    token1.getTokenType().equals(Token.TokenType.VOIDTK)) &&
                    token2.getTokenType().equals(Token.TokenType.IDENFR) &&
                    token3.getTokenType().equals(Token.TokenType.LPARENT)) {
                tokenListIterator.previous(); tokenListIterator.previous(); tokenListIterator.previous();
                funcDefList.add(new FuncDefParser(tokenListIterator, errorTable).parseFuncDef());
            } else {
                break;
            }
            token1 = pickNotNullNextToken(tokenListIterator);
            token2 = pickNotNullNextToken(tokenListIterator);
            token3 = pickNotNullNextToken(tokenListIterator);
        }
        tokenListIterator.previous(); tokenListIterator.previous(); tokenListIterator.previous();
        return funcDefList;
    }
}
