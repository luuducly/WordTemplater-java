package com.github.luuducly.wordtemplater.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExpressionParser {
    public static class ParsedExpression {
        public String funcName = "";
        public String fieldPath = "";
        public List<Object> args = new ArrayList<>();
    }

    public static ParsedExpression parse(String rawExpr) {
        ParsedExpression result = new ParsedExpression();
        if (rawExpr == null) return result;

        String clean = rawExpr.trim().replace("«", "").replace("»", "").trim();
        Pattern pattern = Pattern.compile("^([a-zA-Z0-9_]+)\\s*\\((.*)\\)$");
        Matcher matcher = pattern.matcher(clean);

        if (!matcher.matches()) {
            result.fieldPath = clean;
            return result;
        }

        result.funcName = matcher.group(1).toLowerCase();
        String rawArgs = matcher.group(2).trim();

        if (rawArgs.isEmpty()) {
            return result;
        }

        List<Object> split = splitArguments(rawArgs);
        if (!split.isEmpty()) {
            result.fieldPath = String.valueOf(split.get(0));
            result.args = split.subList(1, split.size());
        }

        return result;
    }

    private static List<Object> splitArguments(String argString) {
        List<Object> args = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = 0;

        for (int i = 0; i < argString.length(); i++) {
            char c = argString.charAt(i);
            if (inQuotes) {
                if (c == quoteChar) {
                    inQuotes = false;
                } else {
                    current.append(c);
                }
            } else {
                if (c == '\'' || c == '"') {
                    inQuotes = true;
                    quoteChar = c;
                } else if (c == ',') {
                    args.add(parseVal(current.toString().trim()));
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
        }

        if (current.length() > 0) {
            args.add(parseVal(current.toString().trim()));
        }

        return args;
    }

    private static Object parseVal(String val) {
        if ("true".equalsIgnoreCase(val)) return true;
        if ("false".equalsIgnoreCase(val)) return false;
        if ("null".equalsIgnoreCase(val)) return null;
        try {
            if (val.contains(".")) return Double.parseDouble(val);
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            return val;
        }
    }
}
