package test.cases.context;

import org.junit.Test;

import test.core.selfrunning.AbstractBoomerangTest;
import test.core.selfrunning.AllocatedObject;

public class AliasViaParameterTest extends AbstractBoomerangTest{
	@Test
	public void aliasViaParameter(){
		A a = new A();
		A b = a;
		setAndLoadFieldOnAlias(a,b);
		AllocatedObject query = a.field;
		queryFor(query);
	}
	@Test
	public void aliasViaParameterWrapped(){
		A a = new A();
		A b = a;
		passThrough(a,b);
		AllocatedObject query = a.field;
		queryFor(query);
	}
	
	private void passThrough(A a, A b) {
		setAndLoadFieldOnAlias(a,b);		
	}
	private void setAndLoadFieldOnAlias(A a, A b) {
		b.field = new AllocatedObject(){};
	}

	private static class A{
		AllocatedObject field;
	}
}
