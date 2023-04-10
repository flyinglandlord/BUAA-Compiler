package frontend.grammar.expr;

import frontend.error.Error;
import frontend.error.ErrorTable;
import frontend.grammar.expr.linked_expr.*;
import frontend.grammar.expr.unary_expr.*;
import frontend.grammar.expr.unary_expr.Number;
import frontend.lexical.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class ExprParser {
    private final ListIterator<Token> tokenListIterator;
    private final ErrorTable errorTable;

    public ExprParser(ListIterator<Token> tokenListIterator, ErrorTable errorTable) {
        this.tokenListIterator = tokenListIterator;
        this.errorTable = errorTable;
    }

    public ExprParser(List<Token> tokenList, ErrorTable errorTable) {
        this(tokenList.listIterator(), errorTable);
    }

    // <Exp> := <AddExp>
    public Exp parseExp() {
        return new Exp(parseAddExp());
    }

    // <Cond> := <LOrExp>
    public Cond parseCond() {
        return new Cond(parseLOrExp());
    }

    public ConstExp parseConstExp() {
        return new ConstExp(parseAddExp());
    }

    // <AddExp> := <MulExp> | <AddExp> ( '+' | '-' ) <MulExp>
    public AddExp parseAddExp() {
        MulExp first = parseMulExp();
        List<MulExp> rest = new ArrayList<>();
        List<Token> linkOperator = new ArrayList<>();
        while (tokenListIterator.hasNext()) {
            Token token = tokenListIterator.next();
            if (token.getTokenType() == Token.TokenType.PLUS || token.getTokenType() == Token.TokenType.MINU) {
                rest.add(parseMulExp());
                linkOperator.add(token);
            } else {
                tokenListIterator.previous();
                break;
            }
        }
        //System.out.println(rest.size());
        return new AddExp(first, rest, linkOperator);
    }

    // <MulExp> := <UnaryExp> | <MulExp> ( '*' | '/' | '%' ) <UnaryExp>
    public MulExp parseMulExp() {
        UnaryExp first = parseUnaryExp();
        List<UnaryExp> rest = new ArrayList<>();
        List<Token> linkOperator = new ArrayList<>();
        while (tokenListIterator.hasNext()) {
            Token token = tokenListIterator.next();
            if (token.getTokenType() == Token.TokenType.MULT ||
                    token.getTokenType() == Token.TokenType.DIV ||
                    token.getTokenType() == Token.TokenType.MOD ||
                    token.getTokenType() == Token.TokenType.BITAND) {
                rest.add(parseUnaryExp());
                linkOperator.add(token);
            } else {
                tokenListIterator.previous();
                break;
            }
        }
        return new MulExp(first, rest, linkOperator);
    }

    // <RelExp> := <AddExp> | <RelExp> ( '<' | '>' | '<=' | '>=' ) <AddExp>
    public RelExp parseRelExp() {

        AddExp first = parseAddExp();
        List<AddExp> rest = new ArrayList<>();
        List<Token> linkOperator = new ArrayList<>();
        while (tokenListIterator.hasNext()) {
            Token token = tokenListIterator.next();
            if (token.getTokenType() == Token.TokenType.LSS ||
                    token.getTokenType() == Token.TokenType.GRE ||
                    token.getTokenType() == Token.TokenType.LEQ ||
                    token.getTokenType() == Token.TokenType.GEQ) {
                rest.add(parseAddExp());
                linkOperator.add(token);
            } else {
                tokenListIterator.previous();
                break;
            }
        }
        return new RelExp(first, rest, linkOperator);
    }

    // <EqExp> := <RelExp> | <EqExp> ( '==' | '!=' ) <RelExp>
    public EqExp parseEqExp() {
        RelExp first = parseRelExp();
        List<RelExp> rest = new ArrayList<>();
        List<Token> linkOperator = new ArrayList<>();
        while (tokenListIterator.hasNext()) {
            Token token = tokenListIterator.next();
            if (token.getTokenType() == Token.TokenType.EQL ||
                    token.getTokenType() == Token.TokenType.NEQ) {
                rest.add(parseRelExp());
                linkOperator.add(token);
            } else {
                tokenListIterator.previous();
                break;
            }
        }
        return new EqExp(first, rest, linkOperator);
    }

    // <LAndExp> := <EqExp> | <LAndExp> '&&' <EqExp>
    public LAndExp parseLAndExp() {
        EqExp first = parseEqExp();
        List<EqExp> rest = new ArrayList<>();
        List<Token> linkOperator = new ArrayList<>();
        while (tokenListIterator.hasNext()) {
            Token token = tokenListIterator.next();
            if (token.getTokenType() == Token.TokenType.AND) {
                rest.add(parseEqExp());
                linkOperator.add(token);
            } else {
                tokenListIterator.previous();
                break;
            }
        }
        return new LAndExp(first, rest, linkOperator);
    }

    // <LOrExp> := <LAndExp> | <LOrExp> '||' <LAndExp>
    public LOrExp parseLOrExp() {
        LAndExp first = parseLAndExp();
        List<LAndExp> rest = new ArrayList<>();
        List<Token> linkOperator = new ArrayList<>();
        while (tokenListIterator.hasNext()) {
            Token token = tokenListIterator.next();
            if (token.getTokenType() == Token.TokenType.OR) {
                rest.add(parseLAndExp());
                linkOperator.add(token);
            } else {
                tokenListIterator.previous();
                break;
            }
        }
        return new LOrExp(first, rest, linkOperator);
    }

    // <FuncCall> := <Ident> '(' [ <FuncRParams> ] ')'
    // 可能出现缺少)
    public FuncCall parseFuncCall() {
        Token ident = tokenListIterator.next();
        Token leftParen = tokenListIterator.next(); // skip '('
        Token rightParen = tokenListIterator.next();
        if (rightParen.getTokenType() == Token.TokenType.RPARENT) {
            return new FuncCall(ident, leftParen, rightParen);
        } else if (rightParen.getTokenType() == Token.TokenType.SEMICN) {
            errorTable.add(new Error(Error.ErrorType.MISSING_RIGHT_PARENT, leftParen.getLinenumber()));
            return new FuncCall(ident, leftParen, null);
        } else tokenListIterator.previous();
        FuncRParams funcRParams = parseFuncRParams();
        rightParen = tokenListIterator.next(); // skip ')'
        if (rightParen.getTokenType() != Token.TokenType.RPARENT) {
            rightParen = null;
            Token lastToken = tokenListIterator.previous();
            errorTable.add(new Error(Error.ErrorType.MISSING_RIGHT_PARENT, lastToken.getLinenumber()));
        }
        return new FuncCall(ident, leftParen, funcRParams, rightParen);
    }

    // <FuncRParams> := <Exp> { ',' <Exp> }
    public FuncRParams parseFuncRParams() {
        Exp first = parseExp();
        List<Exp> rest = new ArrayList<>();
        List<Token> commas = new ArrayList<>();
        while (tokenListIterator.hasNext()) {
            Token token = tokenListIterator.next();
            if (token.getTokenType() == Token.TokenType.COMMA) {
                rest.add(parseExp());
                commas.add(token);
            } else {
                tokenListIterator.previous();
                break;
            }
        }
        return new FuncRParams(first, commas, rest);
    }

    // <UnaryExp> := { <UnaryOp> } ( <UnaryExp> | <PrimaryExp> | <Ident> '(' [ <FuncRParams> ] ')' )
    public UnaryExp parseUnaryExp() {
        List<Token> unaryOp = new ArrayList<>();
        // Parse UnaryOp
        while (tokenListIterator.hasNext()) {
            Token token = tokenListIterator.next();
            if (token.getTokenType() == Token.TokenType.PLUS ||
                    token.getTokenType() == Token.TokenType.MINU ||
                    token.getTokenType() == Token.TokenType.NOT) {
                unaryOp.add(token);
            } else {
                tokenListIterator.previous();
                break;
            }
        }
        // Parse UnaryExpBase
        // identifier + '(' -> function call
        // identifier + '[' | identifier -> LVal -> PrimaryExp
        // 其余情况均为 PrimaryExp 包括 SubExp, LVal, Number
        if (tokenListIterator.hasNext()) {
            Token token = tokenListIterator.next();
            if (token.getTokenType() == Token.TokenType.IDENFR) {
                if (tokenListIterator.hasNext()) {
                    Token nextToken = tokenListIterator.next();
                    if (nextToken.getTokenType() == Token.TokenType.LPARENT) {
                        tokenListIterator.previous();
                        tokenListIterator.previous();
                        return new UnaryExp(parseFuncCall(), unaryOp);
                    } else {
                        tokenListIterator.previous();
                        tokenListIterator.previous();
                        return new UnaryExp(parsePrimaryExp(), unaryOp);
                    }
                } else {
                    tokenListIterator.previous();
                    return new UnaryExp(parsePrimaryExp(), unaryOp);
                }
            } else {
                tokenListIterator.previous();
                return new UnaryExp(parsePrimaryExp(), unaryOp);
            }
        } else {
            throw new RuntimeException("Expect IDENT or PrimaryExp, but get EOF");
        }
    }

    // <SubExpr> := '(' <Exp> ')'
    // 可能出现缺少)
    public SubExpr parseSubExpr() {
        Token leftParen = tokenListIterator.next();
        Exp exp = parseExp();
        Token rightParen = tokenListIterator.next();
        if (rightParen.getTokenType() != Token.TokenType.RPARENT) {
            rightParen = null;
            Token lastToken = tokenListIterator.previous();
            errorTable.add(new Error(Error.ErrorType.MISSING_RIGHT_PARENT, lastToken.getLinenumber()));
        }
        return new SubExpr(leftParen, exp, rightParen);
    }

    // <PrimaryExp> := '(' <Exp> ')' | <LVal> | <Number>
    // '(' -> SubExpr
    // identifier -> LVal
    // IntConst -> Number
    public PrimaryExp parsePrimaryExp() {
        Token token = tokenListIterator.next();
        if (token.getTokenType() == Token.TokenType.LPARENT) {
            tokenListIterator.previous();
            SubExpr subExpr = parseSubExpr();
            return new PrimaryExp(subExpr);
        } else if (token.getTokenType() == Token.TokenType.IDENFR) {
            tokenListIterator.previous();
            return new PrimaryExp(parseLVal());
        } else if (token.getTokenType() == Token.TokenType.INTCON) {
            return new PrimaryExp(new Number(token));
        } else {
            throw new RuntimeException("Expect '(' or LVal or Number, but got " + token);
        }
    }

    // <LVal> := Ident { '[' <Exp> ']' }
    // 可能缺少]
    public LVal parseLVal() {
        Token ident = tokenListIterator.next();
        List<Subscript> subscripts = new ArrayList<>();
        while (tokenListIterator.hasNext()) {
            Token leftBracket = tokenListIterator.next();
            if (leftBracket.getTokenType() == Token.TokenType.LBRACK) {
                Exp index = parseExp();
                Token rightBracket = tokenListIterator.next();
                if (rightBracket.getTokenType() != Token.TokenType.RBRACK) {
                    rightBracket = null;
                    Token lastToken = tokenListIterator.previous();
                    errorTable.add(new Error(Error.ErrorType.MISSING_RIGHT_BRACKET, lastToken.getLinenumber()));
                }
                subscripts.add(new Subscript(leftBracket, rightBracket, index));
            } else {
                tokenListIterator.previous();
                break;
            }
        }
        return new LVal(ident, subscripts);
    }
}
