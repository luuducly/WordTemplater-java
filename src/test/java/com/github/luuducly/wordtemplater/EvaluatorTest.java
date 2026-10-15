package com.github.luuducly.wordtemplater;

import com.github.luuducly.wordtemplater.evaluators.Evaluators.*;
import com.github.luuducly.wordtemplater.utils.ExpressionParser;
import com.github.luuducly.wordtemplater.utils.TokenResolver;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class EvaluatorTest {

    @Test
    public void testStringEvaluators() {
        UpperEvaluator upper = new UpperEvaluator();
        assertEquals("HELLO WORLD", upper.evaluate("hello world", Collections.emptyList()));

        LowerEvaluator lower = new LowerEvaluator();
        assertEquals("hello world", lower.evaluate("HELLO WORLD", Collections.emptyList()));

        TrimEvaluator trim = new TrimEvaluator();
        assertEquals("test", trim.evaluate("  test  ", Collections.emptyList()));

        SubEvaluator sub = new SubEvaluator();
        assertEquals("WORD", sub.evaluate("WORDTEMPLATER", Arrays.asList(0, 4)));

        LeftEvaluator left = new LeftEvaluator();
        assertEquals("INV", left.evaluate("INV-1234", Collections.singletonList(3)));

        RightEvaluator right = new RightEvaluator();
        assertEquals("1234", right.evaluate("INV-1234", Collections.singletonList(4)));

        ReplaceEvaluator replace = new ReplaceEvaluator();
        assertEquals("2026/10/15", replace.evaluate("2026-10-15", Arrays.asList("-", "/")));

        PadLeftEvaluator padLeft = new PadLeftEvaluator();
        assertEquals("00042", padLeft.evaluate(42, Arrays.asList(5, "0")));

        PadRightEvaluator padRight = new PadRightEvaluator();
        assertEquals("A1____", padRight.evaluate("A1", Arrays.asList(6, "_")));
    }

    @Test
    public void testNumberAndCurrency() {
        CurrencyEvaluator currency = new CurrencyEvaluator();
        String usRes = currency.evaluate(1500, Collections.singletonList("en-US")).toString();
        assertTrue(usRes.contains("1,500") || usRes.contains("$"));

        String viRes = currency.evaluate(1500000, Collections.singletonList("vi-VN")).toString();
        assertTrue(viRes.contains("1.500.000") || viRes.contains("₫"));

        NumberEvaluator number = new NumberEvaluator();
        assertEquals("1,234,567", number.evaluate(1234567, Collections.emptyList()));

        PercentageEvaluator pct = new PercentageEvaluator();
        assertEquals("85.00%", pct.evaluate(0.85, Collections.emptyList()));
    }

    @Test
    public void testDateAndLogic() {
        DateEvaluator date = new DateEvaluator();
        assertEquals("15/10/2026", date.evaluate("2026-10-15T00:00:00", Collections.singletonList("dd/MM/yyyy")));

        DefaultValueEvaluator def = new DefaultValueEvaluator();
        assertEquals("N/A", def.evaluate(null, Collections.singletonList("N/A")));
        assertEquals("Valid", def.evaluate("Valid", Collections.singletonList("N/A")));

        IfEvaluator ifEval = new IfEvaluator();
        assertEquals("Yes", ifEval.evaluate(true, Arrays.asList("Yes", "No")));
        assertEquals("No", ifEval.evaluate(false, Arrays.asList("Yes", "No")));
    }

    @Test
    public void testTokenResolverAndParser() {
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> customer = new HashMap<>();
        customer.put("name", "Acme Corporation");
        data.put("Customer", customer);

        assertEquals("Acme Corporation", TokenResolver.resolve(data, "Customer.name"));
        assertEquals("Acme Corporation", TokenResolver.resolve(data, "customer.Name"));

        List<Map<String, Object>> items = new ArrayList<>();
        Map<String, Object> item0 = new HashMap<>();
        item0.put("Title", "Cloud Product");
        items.add(item0);
        data.put("Items", items);

        assertEquals("Cloud Product", TokenResolver.resolve(data, "Items[0].Title"));
        assertEquals("Cloud Product", TokenResolver.resolve(data, "items[0].title"));

        ExpressionParser.ParsedExpression parsed = ExpressionParser.parse("currency(Total, 'vi-VN')");
        assertEquals("currency", parsed.funcName);
        assertEquals("Total", parsed.fieldPath);
        assertEquals(1, parsed.args.size());
        assertEquals("vi-VN", parsed.args.get(0));
    }
}
