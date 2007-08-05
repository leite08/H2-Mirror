/*
 * Copyright 2004-2007 H2 Group. Licensed under the H2 License, Version 1.0 (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.expression;

import java.sql.SQLException;

import org.h2.engine.Constants;
import org.h2.engine.Session;
import org.h2.table.ColumnResolver;
import org.h2.table.TableFilter;
import org.h2.value.Value;
import org.h2.value.ValueNull;

public class ConditionNot extends Condition {

    private Expression condition;

    public ConditionNot(Expression condition) {
        this.condition = condition;
    }

    public Expression getNotIfPossible(Session session) {
        return condition;
    }

    public Value getValue(Session session) throws SQLException {
        Value v = condition.getValue(session);
        if(v == ValueNull.INSTANCE) {
            return v;
        }
        return v.convertTo(Value.BOOLEAN).negate();
    }

    public void mapColumns(ColumnResolver resolver, int level) throws SQLException {
        condition.mapColumns(resolver, level);
    }

    public Expression optimize(Session session) throws SQLException {
        if(!Constants.OPTIMIZE_NOT) {
            return this;
        }
        // TODO optimization: some cases are maybe possible to optimize further: (NOT ID >= 5)
        Expression expr = condition.optimize(session);
        Expression e2 = expr.getNotIfPossible(session);
        if(e2 != null) {
            return e2.optimize(session);
        }
        if(expr.isConstant()) {
            Value v = expr.getValue(session);
            if(v == ValueNull.INSTANCE) {
                return ValueExpression.NULL;
            }
            return ValueExpression.get(v.convertTo(Value.BOOLEAN).negate());
        }
        condition = expr;
        return this;
    }

    public void setEvaluatable(TableFilter tableFilter, boolean b) {
        condition.setEvaluatable(tableFilter, b);
    }

    public String getSQL() {
        return "(NOT " + condition.getSQL() + ")";
    }

    public void updateAggregate(Session session) throws SQLException {
        condition.updateAggregate(session);
    }
    
    public void addFilterConditions(TableFilter filter, boolean outerJoin) {
        if(outerJoin) {
            // can not optimize:
            // select * from test t1 left join test t2 on t1.id = t2.id where not t2.id is not null
            // to
            // select * from test t1 left join test t2 on t1.id = t2.id and t2.id is not null
            return;
        }
        super.addFilterConditions(filter, outerJoin);
    }

    public boolean isEverything(ExpressionVisitor visitor) {
        return condition.isEverything(visitor);
    }      
    
    public int getCost() {
        return condition.getCost();
    }

}
