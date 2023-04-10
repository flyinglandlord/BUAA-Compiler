package frontend.grammar.expr.linked_expr;

import frontend.grammar.Node;
import frontend.lexical.Token;

import java.io.PrintStream;
import java.util.List;

public class LinkedExpr<T extends Node> implements Node {
    private final T first;
    private final List<Token> linkOperator;
    private final List<T> rest;
    private final String classTName;

    public LinkedExpr(T first, List<Token> linkOperator, List<T> rest, String classTName) {
        this.first = first;
        this.linkOperator = linkOperator;
        this.rest = rest;
        this.classTName = classTName;
    }

    public T getFirst() {
        return first;
    }

    public List<Token> getLinkOperator() {
        return linkOperator;
    }

    public List<T> getRest() {
        return rest;
    }

    @Override
    public void print(PrintStream out) {
        first.print(out);
        out.println(classTName);
        for (int i = 0; i < linkOperator.size(); i++) {
            out.println(linkOperator.get(i).toString());
            rest.get(i).print(out);
            out.println(classTName);
        }
    }
}
