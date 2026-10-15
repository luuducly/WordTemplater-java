package com.github.luuducly.wordtemplater.evaluators;

import com.github.luuducly.wordtemplater.IEvaluator;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Evaluators {

    public static class UpperEvaluator implements IEvaluator {
        @Override
        public Object evaluate(Object value, List<Object> parameters) {
            return value != null ? value.toString().toUpperCase() : "";
        }
    }

    public static class LowerEvaluator implements IEvaluator {
        @Override
        public Object evaluate(Object value, List<Object> parameters) {
            return value != null ? value.toString().toLowerCase() : "";
        }
    }

    public static class TrimEvaluator implements IEvaluator {
        @Override
        public Object evaluate(Object value, List<Object> parameters) {
            return value != null ? value.toString().trim() : "";
        }
    }

    public static class SubEvaluator implements IEvaluator {
        @Override
        public Object evaluate(Object value, List<Object> parameters) {
            if (value == null) return "";
            String s = value.toString();
            int start = parameters.size() > 0 ? Integer.parseInt(parameters.get(0).toString()) : 0;
            if (parameters.size() > 1) {
                int length = Integer.parseInt(parameters.get(1).toString());
                return s.substring(start, Math.min(s.length(), start + length));
            }
            return s.substring(start);
        }
    }

    public static class LeftEvaluator implements IEvaluator {
        @Override
        public Object evaluate(Object value, List<Object> parameters) {
            if (value == null) return "";
            String s = value.toString();
            int count = parameters.size() > 0 ? Integer.parseInt(parameters.get(0).toString()) : 0;
            return s.substring(0, Math.min(s.length(), Math.max(0, count)));
        }
    }

    public static class RightEvaluator implements IEvaluator {
        @Override
        public Object evaluate(Object value, List<Object> parameters) {
            if (value == null) return "";
            String s = value.toString();
            int count = parameters.size() > 0 ? Integer.parseInt(parameters.get(0).toString()) : 0;
            if (count <= 0) return "";
            if (count >= s.length()) return s;
            return s.substring(s.length() - count);
        }
    }

    public static class ReplaceEvaluator implements IEvaluator {
        @Override
        public Object evaluate(Object value, List<Object> parameters) {
            if (value == null) return "";
            String s = value.toString();
            String oldVal = parameters.size() > 0 ? parameters.get(0).toString() : "";
            String newVal = parameters.size() > 1 ? parameters.get(1).toString() : "";
            return s.replace(oldVal, newVal);
        }
    }

    public static class PadLeftEvaluator implements IEvaluator {
        @Override
        public Object evaluate(Object value, List<Object> parameters) {
            if (value == null) return "";
            String s = value.toString();
            int width = parameters.size() > 0 ? Integer.parseInt(parameters.get(0).toString()) : s.length();
            char padChar = (parameters.size() > 1 && !parameters.get(1).toString().isEmpty())
                    ? parameters.get(1).toString().charAt(0) : ' ';
            if (s.length() >= width) return s;
            StringBuilder sb = new StringBuilder();
            while (sb.length() < width - s.length()) {
                sb.append(padChar);
            }
            sb.append(s);
            return sb.toString();
        }
    }

    public static class PadRightEvaluator implements IEvaluator {
        @Override
        public Object evaluate(Object value, List<Object> parameters) {
            if (value == null) return "";
            String s = value.toString();
            int width = parameters.size() > 0 ? Integer.parseInt(parameters.get(0).toString()) : s.length();
            char padChar = (parameters.size() > 1 && !parameters.get(1).toString().isEmpty())
                    ? parameters.get(1).toString().charAt(0) : ' ';
            if (s.length() >= width) return s;
            StringBuilder sb = new StringBuilder(s);
            while (sb.length() < width) {
                sb.append(padChar);
            }
            return sb.toString();
        }
    }

    public static class CurrencyEvaluator implements IEvaluator {
        @Override
        public Object evaluate(Object value, List<Object> parameters) {
            if (value == null || value.toString().isEmpty()) return "";
            try {
                double val = Double.parseDouble(value.toString());
                String culture = parameters.size() > 0 ? parameters.get(0).toString() : "en-US";
                if (culture.toLowerCase().startsWith("vi")) {
                    DecimalFormat df = new DecimalFormat("#,##0 ₫");
                    return df.format(val).replace(",", ".");
                }
                NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.forLanguageTag(culture));
                return nf.format(val);
            } catch (Exception e) {
                return value.toString();
            }
        }
    }

    public static class NumberEvaluator implements IEvaluator {
        @Override
        public Object evaluate(Object value, List<Object> parameters) {
            if (value == null || value.toString().isEmpty()) return "";
            try {
                double val = Double.parseDouble(value.toString());
                String culture = parameters.size() > 1 ? parameters.get(1).toString() : "en-US";
                NumberFormat nf = NumberFormat.getNumberInstance(Locale.forLanguageTag(culture));
                if (val == (long) val) {
                    return nf.format((long) val);
                }
                return nf.format(val);
            } catch (Exception e) {
                return value.toString();
            }
        }
    }

    public static class PercentageEvaluator implements IEvaluator {
        @Override
        public Object evaluate(Object value, List<Object> parameters) {
            if (value == null || value.toString().isEmpty()) return "";
            try {
                double val = Double.parseDouble(value.toString());
                return String.format("%.2f%%", val * 100);
            } catch (Exception e) {
                return value.toString();
            }
        }
    }

    public static class DateEvaluator implements IEvaluator {
        @Override
        public Object evaluate(Object value, List<Object> parameters) {
            if (value == null || value.toString().isEmpty()) return "";
            String fmt = parameters.size() > 0 ? parameters.get(0).toString() : "yyyy-MM-dd";
            try {
                if (value instanceof Date) {
                    return new SimpleDateFormat(fmt).format((Date) value);
                }
                if (value instanceof LocalDateTime) {
                    return ((LocalDateTime) value).format(DateTimeFormatter.ofPattern(fmt));
                }
                if (value instanceof LocalDate) {
                    return ((LocalDate) value).format(DateTimeFormatter.ofPattern(fmt));
                }
                String str = value.toString();
                if (str.contains("T")) {
                    LocalDateTime dt = LocalDateTime.parse(str.replace("Z", ""));
                    return dt.format(DateTimeFormatter.ofPattern(fmt));
                }
                LocalDate d = LocalDate.parse(str);
                return d.format(DateTimeFormatter.ofPattern(fmt));
            } catch (Exception e) {
                return value.toString();
            }
        }
    }

    public static class DefaultValueEvaluator implements IEvaluator {
        @Override
        public Object evaluate(Object value, List<Object> parameters) {
            if (value == null || value.toString().trim().isEmpty()) {
                return parameters.size() > 0 ? parameters.get(0) : "";
            }
            return value;
        }
    }

    public static class IfEvaluator implements IEvaluator {
        @Override
        public Object evaluate(Object value, List<Object> parameters) {
            boolean isTruthy = false;
            if (value instanceof Boolean) {
                isTruthy = (Boolean) value;
            } else if (value != null) {
                String s = value.toString().trim().toLowerCase();
                isTruthy = !s.isEmpty() && !s.equals("false") && !s.equals("0");
            }
            if (isTruthy) {
                return parameters.size() > 0 ? parameters.get(0) : "";
            }
            return parameters.size() > 1 ? parameters.get(1) : "";
        }
    }
}
