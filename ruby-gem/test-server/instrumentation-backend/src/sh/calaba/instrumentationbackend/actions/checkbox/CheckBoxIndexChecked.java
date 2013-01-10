package sh.calaba.instrumentationbackend.actions.checkbox;


import sh.calaba.instrumentationbackend.InstrumentationBackend;
import sh.calaba.instrumentationbackend.Result;
import sh.calaba.instrumentationbackend.actions.Action;


public class CheckBoxIndexChecked implements Action{

	@Override
	public Result execute(String... args) {
		final int firstArgument = Integer.parseInt(args[0]) - 1;
        
		Result result = new Result(true, "Checkbox checked");
		
		boolean isChecked = InstrumentationBackend.solo.isCheckBoxChecked(firstArgument);
        
    	if (isChecked) {
    		result.addBonusInformation("Yes");
    		System.out.println("checkbox is checked");
    	} else {
    		result.addBonusInformation("No");
    		System.out.println("Checkbox is not checked");
    	}
        
        return result;
	}

	@Override
	public String key() {
		return "checkbox_index_checked";
	}
	
	

}
