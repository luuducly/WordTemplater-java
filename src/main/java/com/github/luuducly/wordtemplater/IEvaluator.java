package com.github.luuducly.wordtemplater;

import java.util.List;

public interface IEvaluator {
    Object evaluate(Object value, List<Object> parameters);
}
