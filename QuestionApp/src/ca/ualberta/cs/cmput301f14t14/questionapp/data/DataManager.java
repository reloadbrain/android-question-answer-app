package ca.ualberta.cs.cmput301f14t14.questionapp.data;

import java.util.List;
import java.util.UUID;

import android.content.Context;

import ca.ualberta.cs.cmput301f14t14.questionapp.data.eventbus.EventBus;
import ca.ualberta.cs.cmput301f14t14.questionapp.data.eventbus.events.AbstractEvent;
import ca.ualberta.cs.cmput301f14t14.questionapp.data.eventbus.events.AnswerCommentPushDelayedEvent;
import ca.ualberta.cs.cmput301f14t14.questionapp.data.eventbus.events.AnswerPushDelayedEvent;
import ca.ualberta.cs.cmput301f14t14.questionapp.data.eventbus.events.QuestionCommentPushDelayedEvent;
import ca.ualberta.cs.cmput301f14t14.questionapp.data.eventbus.events.QuestionPushDelayedEvent;
import ca.ualberta.cs.cmput301f14t14.questionapp.data.threading.AddAnswerCommentTask;
import ca.ualberta.cs.cmput301f14t14.questionapp.data.threading.AddAnswerTask;
import ca.ualberta.cs.cmput301f14t14.questionapp.data.threading.AddQuestionCommentTask;
import ca.ualberta.cs.cmput301f14t14.questionapp.data.threading.AddQuestionTask;
import ca.ualberta.cs.cmput301f14t14.questionapp.data.threading.GetAnswerCommentTask;
import ca.ualberta.cs.cmput301f14t14.questionapp.data.threading.GetAnswerListTask;
import ca.ualberta.cs.cmput301f14t14.questionapp.data.threading.GetAnswerTask;
import ca.ualberta.cs.cmput301f14t14.questionapp.data.threading.GetCommentListAnsTask;
import ca.ualberta.cs.cmput301f14t14.questionapp.data.threading.GetCommentListQuesTask;
import ca.ualberta.cs.cmput301f14t14.questionapp.data.threading.GetQuestionCommentTask;
import ca.ualberta.cs.cmput301f14t14.questionapp.data.threading.GetQuestionListTask;
import ca.ualberta.cs.cmput301f14t14.questionapp.data.threading.GetQuestionTask;
import ca.ualberta.cs.cmput301f14t14.questionapp.data.threading.UpvoteQuestionTask;
import ca.ualberta.cs.cmput301f14t14.questionapp.model.Answer;
import ca.ualberta.cs.cmput301f14t14.questionapp.model.Comment;
import ca.ualberta.cs.cmput301f14t14.questionapp.model.Question;

/**
 * DataManager is a singleton that talks to local and remote data sources
 */
public class DataManager {
	
	private static DataManager instance;

	private IDataStore localDataStore;
	private IDataStore remoteDataStore;
	private List<UUID> favouriteQuestions;
	private List<UUID> favouriteAnswers;
	private List<UUID> recentVisit;
	private List<UUID> readLater;
	//private List<UUID> pushOnline;
	//private List<UUID> upVoteOnline;
	private Context context; //Needed for Threading instantiations
	String Username;
	
	private EventBus eventbus = EventBus.getInstance();

	
	private DataManager(Context context) {
		//Deprecated. Use the eventbus instead for events that need to happen
		//upon future internet access
		//this.pushOnline = new ArrayList<UUID>();
		//this.upVoteOnline = new ArrayList<UUID>();
		this.context = context;
	}

	/**
	 * Create data stores
	 * 
	 * This must be done after the constructor, because some data stores
	 * refer back to DataManager, and cannot do so until it is constructed.
	 */
	private void initDataStores() {
		this.localDataStore = new LocalDataStore(context);
		this.remoteDataStore = new RemoteDataStore(context);
	}

	/**
	 * Singleton getter
	 * @param context
	 * @return Instance of DataManager
	 */
	public static DataManager getInstance(Context context){
		if (instance == null){
			instance = new DataManager(context.getApplicationContext());
			instance.initDataStores();
		}
		
		return instance;
	}
	
	private void completeQueuedEvents() {
		//The singleton eventbus contains events that attempted to 
		//be posted to the internet. If posting failed, an event was created
		//on the eventbus. These queued events should regularly "tried again"
		//so that we are as frequently as possible trying to update the internet
		//with our new local information.
		//I believe this is the magic that is currently missing to make the DataManager
		//transparently update the local and remote stores.
		
		//For each event in the event bus, try and do it again.
		for (AbstractEvent e: eventbus.getEventQueue()){				
			/* Remove the current event from the eventbus. If "trying again" fails,
			 * it will happen in a separate thread, and it will again be added to the bus
			 */
			eventbus.removeEvent(e);
			
			if (e instanceof QuestionPushDelayedEvent) {
				//try pushing the question again
				addQuestion(((QuestionPushDelayedEvent) e).q, null);
			}
			if (e instanceof AnswerPushDelayedEvent) {
				addAnswer(((AnswerPushDelayedEvent) e).a);
			}
			if (e instanceof QuestionCommentPushDelayedEvent) {
				addQuestionComment(((QuestionCommentPushDelayedEvent) e).qc);
			}
			if (e instanceof AnswerCommentPushDelayedEvent) {
				addAnswerComment(((AnswerCommentPushDelayedEvent)e).ca);
			}
		}
	}
	
	//View Interface Begins
	public void addQuestion(Question validQ, Callback<Void> c) {
		AddQuestionTask aqt = new AddQuestionTask(context);
		aqt.setCallBack(c);
		aqt.execute(validQ);
	}

	/**
	 * Get a question by its UUID
	 * @param id
	 * @return
	 */
	public Question getQuestion(UUID id, Callback<Question> c) {
		GetQuestionTask task = new GetQuestionTask(context);
		if (c == null) {
			return task.blockingRun(id);
		}
		task.setCallBack(c);
		task.execute(id);
		return null;
		 
	}
	
	/**
	 * Add an answer record
	 * @param A Answer to add
	 */
	public void addAnswer(Answer A){
		AddAnswerTask aat = new AddAnswerTask(context);
		aat.blockingRun(A);
	}

	/**
	 * Get answer record
	 * @param Aid Answer ID
	 * @return
	 */
	public Answer getAnswer(UUID Aid, Callback<Answer> c) {
		//Add this answer to the recentVisit list
		GetAnswerTask gat = new GetAnswerTask(context);
		Answer anull = null;
		if (c == null) {
			//User wants an answer within a thread, or doesn't care about blocking.
			return gat.blockingRun(Aid);
		}
		gat.setCallBack(new Callback<Answer>() {
			@Override
			public void run(Answer a) {
				recentVisit.add(a.getId());
			}
		});
		gat.execute(Aid);
		//Now actually use the callback that the caller wanted
		gat.setCallBack(c);
		gat.execute(Aid);
		return anull; //Hopefully eclipse will warn users this method always returns null
	}

	/**
	 * Add comment record to question
	 * @param C
	 */
	public void addQuestionComment(Comment<Question> C){
		AddQuestionCommentTask aqct = new AddQuestionCommentTask(context);
		aqct.execute(C); //May have a problem here. Look here first if crashing.
	}

	/**
	 * Get comment record from question
	 * @param cid
	 * @return
	 */
	//Wtf, when I added a Callback parameter, nothing broke... Is this 
	//method actually called anywhere in the app?
	public Comment<Question> getQuestionComment(UUID cid, Callback<Comment<Question>> c) {
		GetQuestionCommentTask gqct = new GetQuestionCommentTask(context);
		if (c == null){
			//User does not care about blocking
			return gqct.blockingRun(cid);
		}
		//User cares about threading
		//Add this questionComment to the recentVisit list
		gqct.setCallBack(new Callback<Comment<Question>>() {
			@Override
			public void run(Comment<Question> cq) {
				readLater.add(cq.getId());
			}
		});
		gqct.execute(cid);
		//Now run with the callback the user wanted
		gqct.setCallBack(c);
		gqct.execute(cid);
		//If the user is using threading, they will care to extract their result from the callback
		return null;
		
	}

	/**
	 * Add comment record for answer
	 * @param C
	 */
	public void addAnswerComment(Comment<Answer> C){
		AddAnswerCommentTask aact = new AddAnswerCommentTask(context);
		aact.execute(C);  //Possibly trouble here.
	}

	/**
	 * Get comment record from answer
	 * @param Cid
	 * @return
	 */
	//Another case where adding a callback to the function signature didn't break the app
	//Are we using this?
	public Comment<Answer> getAnswerComment(UUID Cid, Callback<Comment<Answer>> c){
		GetAnswerCommentTask gact = new GetAnswerCommentTask(context);
		if (c == null) {
			//User doesn't care about threading and expects this to be blocking.
			return gact.blockingRun(Cid);
		}
		//Need to add this to the recentVisit list.
		gact.setCallBack(new Callback<Comment<Answer>>() {
			@Override
			public void run(Comment<Answer> ca) {
				recentVisit.add(ca.getId());
				
			}
		});
		gact.execute(Cid);
		//Now run with the callback the user actually wanted
		gact.setCallBack(c);
		gact.execute(Cid);
		//The user, by not setting a null callback, should know to fetch the result 
		//out of the callback, and should not be surprised at an NPE.
		return null;
	}

	/**
	 * Get a list of all existing questions.
	 * 
	 * This list is not returned with any particular order.
	 * @return
	 */
	public List<Question> getQuestionList(Callback callback) {
		GetQuestionListTask task = new GetQuestionListTask(context);
		if (callback == null) {
			//User doesn't care this is blocking
			return task.blockingRun();
		}
		task.setCallBack(callback);
		task.execute();
		//User should expect this to be null, since the result should be pulled out of the callback
		return null;
	}

	/**
	 * Get a list of comments from an answer asynchronously
	 * @param a
	 * @param c
	 * @return
	 */
	public List<Comment<Answer>> getCommentList(Answer a, Callback<List<Comment<Answer>>> c){
		GetCommentListAnsTask gclat = new GetCommentListAnsTask(context);
		if (c == null) {
			//User doesn't care this is blocking
			return gclat.blockingRun(a);
		}
		gclat.setCallBack(c);
		gclat.execute(a);
		//User should expect this to be null, since the result should be pulled out of the callback
		return null;
	}
	
	/**
	 * Get a list of comments from a question asynchronously
	 * @param q
	 * @param c
	 * @return
	 */
	public List<Comment<Question>> getCommentList(Question q, Callback<List<Comment<Question>>> c){
		GetCommentListQuesTask gclqt = new GetCommentListQuesTask(context);
		if (c == null) {
			//User doesn't care this is blocking
			return gclqt.blockingRun(q);
		}
		gclqt.setCallBack(c);
		gclqt.execute(q);
		//User should pull result out of callback
		return null;
	}
	
	public List<Answer> getAnswerList(Question q, Callback<List<Answer>> c){
		GetAnswerListTask galt = new GetAnswerListTask(context);
		if (c == null) {
			//User does not care this is blocking
			return galt.blockingRun(q);
			
		}
		galt.setCallBack(c);
		galt.execute(q);
		//User should pull result out of callback
		return null;
	}
	
	public void upvoteQuestion(Question q){
		UpvoteQuestionTask uqt = new UpvoteQuestionTask(context);
		uqt.execute(q);
		
	}

	public IDataStore getLocalDataStore() {
		return localDataStore;
	}

	public IDataStore getRemoteDataStore() {
		return remoteDataStore;
	}

}
