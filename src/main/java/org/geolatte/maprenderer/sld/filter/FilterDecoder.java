/*
 * This file is part of the GeoLatte project.
 *
 *     GeoLatte is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     GeoLatte is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with GeoLatte.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Copyright (C) 2010 - 2011 and Ownership of code is shared by:
 *  Qmino bvba - Esperantolaan 4 - 3001 Heverlee  (http://www.qmino.com)
 *  Geovise bvba - Generaal Eisenhowerlei 9 - 2140 Antwerpen (http://www.geovise.com)
 */

package org.geolatte.maprenderer.sld.filter;

import net.opengis.filter.v_1_1_0.*;
import org.geolatte.maprenderer.util.JAXBHelper;

import javax.xml.bind.JAXBElement;

public class FilterDecoder {

    private JAXBElement<?> expressionRoot;

    public FilterDecoder(FilterType filterType) {
        if (filterType.getComparisonOps() != null) {
            this.expressionRoot = filterType.getComparisonOps();
            return;
        }
        throw new UnsupportedOperationException();
    }

    public SLDRuleFilter decode() {
        Expression<Boolean, ?> expression = (Expression<Boolean, ?>) parse(expressionRoot);

        SLDRuleFilter filter = new SLDRuleFilter();
        filter.setFilterExpr(expression);
        return filter;
    }

    private Expression<?, ?> parse(JAXBElement<?> element) {

        if (element.getDeclaredType() == BinaryComparisonOpType.class) {
            Comparison comparison = Comparison.valueOf(element.getName().getLocalPart());
            return binaryComparison((BinaryComparisonOpType) element.getValue(), comparison);
        }

        if (element.getDeclaredType() == PropertyNameType.class) {
            return propertyName((PropertyNameType) element.getValue());
        }

        if (element.getDeclaredType() == LiteralType.class) {
            return literal((LiteralType) element.getValue());
        }

        if (element.getDeclaredType() == PropertyIsLikeType.class) {
            return propertyIsLike((PropertyIsLikeType) element.getValue());
        }


        throw new UnsupportedOperationException(element.getDeclaredType().getSimpleName());
    }


    private BinaryComparisonOp binaryComparison(BinaryComparisonOpType operator, Comparison comparison) {
        BinaryComparisonOp op = new BinaryComparisonOp(comparison, operator.isMatchCase());
        int i = 0;
        for (JAXBElement<?> expression : operator.getExpression()) {
            Expression<Object, ?> arg = (Expression<Object, Object>) parse(expression);
            op.setArg(i++, arg);
        }
        return op;
    }

    private LiteralExpression literal(LiteralType literal) {
        LiteralExpression expression = new LiteralExpression();
        expression.SetValue(JAXBHelper.extractValueToString(literal.getContent()));
        return expression;
    }

    private PropertyIsLikeOp propertyIsLike(PropertyIsLikeType element) {
        PropertyIsLikeOp op = new PropertyIsLikeOp(element.getWildCard(), element.getSingleChar(), element.getEscapeChar());
        PropertyExpression property = propertyName(element.getPropertyName());
        LiteralExpression literal = literal(element.getLiteral());
        op.setProperty(property);
        op.setLiteral(literal);
        return op;
    }
//
//    public void addBinaryLogicOp(String operator) {
//        BinaryLogicOp op;
//        if (operator.equalsIgnoreCase("or")) {
//            op = new OrLogicOp();
//        } else {
//            op = new AndLogicOp();
//        }
//        filterTokens.add(op);
//
//    }
//
//    public void addIsBetween() {
//        throw new UnsupportedOperationException("Not implemented");
//
//    }
//
//    public void addIsLikeOperator(String wildCard, String singleChar,
//                                  String escape) {
//        throw new UnsupportedOperationException("Not implemented");
//
//    }
//
//    public void addIsNull() {
//        throw new UnsupportedOperationException("Not implemented");
//    }
//
//    public void addNegation() {
//        NegationExpression op = new NegationExpression();
//        filterTokens.add(op);
//    }


    private PropertyExpression propertyName(PropertyNameType propertyElement) {
        PropertyExpression property = new PropertyExpression();
        String propertyName = JAXBHelper.extractValueToString(propertyElement.getContent());
        property.setPropertyName(propertyName);
        return property;
    }

}
