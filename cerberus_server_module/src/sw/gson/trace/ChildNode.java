package sw.gson.trace;

//6

public class ChildNode implements ChildFactory {

	private String index;
	private String self;
	private String calls;
	private int usec;
	private String methodName;

	public void setChild(String _self, String _index, String _calls,
			String _usecs, String _name, String _name2) {
		
		this.index = _index;
		this.self = _self;
		this.calls = _calls;
		this.usec = Integer.parseInt(_usecs);
		this.methodName = _name + " " + _name2;		
	}

	@Override
	public void setChild2(String _index, String _calls, String _usecs,
			String _name, String _name2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setMain(String _index, String _total, String _usecs) {
		// TODO Auto-generated method stub
		
	}
}
