package sh.calaba.instrumentationbackend.query.ast;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;

import android.os.ConditionVariable;
import android.view.View;

import sh.calaba.instrumentationbackend.query.antlr.UIQueryLexer;
import sh.calaba.instrumentationbackend.query.antlr.UIQueryParser;

public class UIQueryEvaluator {

	private static class UIQueryResultVoid {
		public static final UIQueryResultVoid instance = new UIQueryResultVoid();
		
		private UIQueryResultVoid() {}
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public Object asMap(String methodName, Object receiver,String errorMessage) {
			Map map = new HashMap();
			map.put("error",errorMessage);
			map.put("methodName",methodName);
			map.put("receiverClass", receiver.getClass().getName());
			map.put("receiverString",receiver.toString());
			return map;
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static List evaluateQueryWithOptions(String query, List inputViews,
			List options, ConditionVariable computationFinished) {
		
        long before = System.currentTimeMillis();
        List views = evaluateQueryForPath(parseQuery(query), inputViews, computationFinished);
        long after = System.currentTimeMillis();
        String action = "EvaluateQueryNoOptions";
        System.out.println(action+ " took: "+ (after-before) + "ms");

		

		List result = views;
		/* TODO move to post process
		for (Object methodNameObj : options) {
			String propertyName = (String) methodNameObj;
			List nextResult = new ArrayList(views.size());
			for (Object o : result) {
				try {
					
					before = System.currentTimeMillis();
					if (o instanceof Map)
					{
						Map objAsMap = (Map) o;
						if (objAsMap.containsKey(propertyName))
						{
							nextResult.add(objAsMap.get(propertyName));
						}
						else 
						{
							nextResult.add(UIQueryResultVoid.instance.asMap(propertyName,o,"No key for "+propertyName + ". Keys: "+(objAsMap.keySet().toString())));
						}						
					}
					else 
					{
						if (o instanceof View && "id".equals(propertyName))
						{
							nextResult.add(UIQueryUtils.getId((View) o));
						}
						else 
						{
							Method m = UIQueryUtils.hasProperty(o, propertyName);
							after = System.currentTimeMillis();
					        action = "HasProperty";
					        System.out.println(action+ " took: "+ (after-before) + "ms");
							if (m != null) {
								nextResult.add(m.invoke(o));	
							}
							else 
							{
								nextResult.add(UIQueryResultVoid.instance.asMap(propertyName,o,"NO accessor for "+propertyName));
							}							
						}
					}
					
										
				} catch (Exception e) {
					System.out.println(e.getMessage());
					nextResult.add(UIQueryResultVoid.instance.asMap(propertyName,o,e.getMessage()));
				}
			}
			result = nextResult;
		}
		*/
		return result;

	}

	@SuppressWarnings("unchecked")
	public static List<UIQueryAST> parseQuery(String query) {
		UIQueryLexer lexer = new UIQueryLexer(new ANTLRStringStream(query));
		UIQueryParser parser = new UIQueryParser(new CommonTokenStream(lexer));

		UIQueryParser.query_return q;
		try {
			q = parser.query();
		} catch (RecognitionException e) {
			throw new InvalidUIQueryException(e.getMessage());
		}
		if (q == null) {
			throw new InvalidUIQueryException(query);
		}
		CommonTree rootNode = (CommonTree) q.getTree();
		List<CommonTree> queryPath = rootNode.getChildren();

		if (queryPath == null || queryPath.isEmpty()) {
			queryPath = Collections.singletonList(rootNode);
		}

		return mapUIQueryFromAstNodes(queryPath);
	}

	@SuppressWarnings("rawtypes")
	private static List evaluateQueryForPath(List<UIQueryAST> queryPath,
			List inputViews, ConditionVariable computationFinished) {

		List currentResult = inputViews;
		UIQueryDirection currentDirection = UIQueryDirection.DESCENDANT;
		for (UIQueryAST step : queryPath) {			
			if (isDirection(step)) {
				currentDirection = directionFromAst(step);
			} else {
				System.out.println(step);
		        long before = System.currentTimeMillis();
		        		        

				currentResult = step.evaluateWithViewsAndDirection(
						currentResult, currentDirection,computationFinished);
				long after = System.currentTimeMillis();
		        String action = "EvaluateQueryNoOptions" + step.toString();
		        System.out.println(action+ " took: "+ (after-before) + "ms");

			}

		}
		return currentResult;
	}

	public static UIQueryDirection directionFromAst(UIQueryAST step) {
		// TODO Auto-generated method stub
		return null;
	}

	public static boolean isDirection(UIQueryAST step) {
		// TODO Auto-generated method stub
		return false;
	}

	public static List<UIQueryAST> mapUIQueryFromAstNodes(List<CommonTree> nodes) {
		List<UIQueryAST> mapped = new ArrayList<UIQueryAST>(nodes.size());
		for (CommonTree t : nodes) {
			mapped.add(uiQueryFromAst(t));
		}
		return mapped;
	}

	public static UIQueryAST uiQueryFromAst(CommonTree step) {
		String stepType = UIQueryParser.tokenNames[step.getType()];
		switch (step.getType()) {
		case UIQueryParser.QUALIFIED_NAME:
			try {
				return new UIQueryASTClassName(Class.forName(step.getText()));
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				throw new InvalidUIQueryException("Qualified class name: "
						+ step.getText() + " not found. (" + e.getMessage()
						+ ")");
			}
		case UIQueryParser.NAME:
			return new UIQueryASTClassName(step.getText());

		case UIQueryParser.FILTER_COLON:
			return UIQueryASTWith.fromAST(step);
		default:
			throw new InvalidUIQueryException("Unknown query: " + stepType
					+ " with text: " + step.getText());

		}

	}

}
