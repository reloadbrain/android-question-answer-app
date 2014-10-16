package ca.ualberta.cs.cmput301f14t14.questionapp.model;

import ca.ualberta.cs.cmput301f14t14.questionapp.view.IView;

public class Question extends Model {
	
	private String title;
	private String body;
	private Image image;
	
	public Question(String t, String b, Image i) {
		super();
		title = t;
		body = b;
		image = i;
	}
	
	public void addAnswer(Answer a) {
		
	}
	
	public boolean hasAnswer(Answer a) {
		return false;
	}

	public String getTitle() {
		return title;
	}
	
	public String getBody() {
		return body;
	}
	
	public Image getImage() {
		return image;
	}
	
	@Override
	public void registerView(IView v) {
		// TODO Auto-generated method stub

	}

	@Override
	public void unregisterView(IView v) {
		// TODO Auto-generated method stub

	}

}
