/**
    Copyright 2014-2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.

    Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with the License. A copy of the License is located at

        http://aws.amazon.com/apache2.0/

    or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package session;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SessionEndedRequest;
import com.amazon.speech.speechlet.SessionStartedRequest;
import com.amazon.speech.speechlet.Speechlet;
import com.amazon.speech.speechlet.SpeechletException;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SimpleCard;
import com.omertron.omdbapi.OMDBException;
import com.omertron.omdbapi.OmdbApi;
import com.omertron.omdbapi.model.OmdbVideoFull;
import com.omertron.omdbapi.tools.OmdbBuilder;

public class SessionSpeechlet implements Speechlet {
    private static final Logger log = LoggerFactory.getLogger(SessionSpeechlet.class);

    private static final String MOVIE_NAME_SLOT = "MovieName";
    private static final String[] movieNameWords = {"FirstWord", "SecondWord", "ThirdWord",
            "FourthWord", "FifthWord","SixthWord", "SeventhWord"};
    private static final String MOVIE_NAME_FIRST_WORD = "MovieNameFirstWord";
    private static final String MOVIE_NAME_SECOND_WORD = "MovieNameSecondWord";

    @Override
    public void onSessionStarted(final SessionStartedRequest request, final Session session)
            throws SpeechletException {
        log.info("onSessionStarted requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());
        // any initialization logic goes here
    }

    @Override
    public SpeechletResponse onLaunch(final LaunchRequest request, final Session session)
            throws SpeechletException {
        log.info("onLaunch requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());
        return getWelcomeResponse();
    }

    @Override
    public SpeechletResponse onIntent(final IntentRequest request, final Session session)
            throws SpeechletException {
        log.info("onIntent requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());

        // Get intent from the request object.
        Intent intent = request.getIntent();
        String intentName = (intent != null) ? intent.getName() : null;

        // Note: If the session is started with an intent, no welcome message will be rendered;
        // rather, the intent specific response will be returned.
        if ("WhatsMovieRatingIntent".equals(intentName)) {
            return getMovieRating(intent, session);
        }
        else if ("AMAZON.HelpIntent".equals(intentName)) {
            return getHelpResponse();
        }
        else if ("AMAZON.StopIntent".equals(intentName) || "AMAZON.CancelIntent".equals(intentName)) {
            return getGoodByResponse();
        }
        else if ("MovieCastIntent".equals(intentName)) {
            return getCastInformation(intent, session);
        }
        else {
            throw new SpeechletException("Invalid Intent");
        }
    }

    @Override
    public void onSessionEnded(final SessionEndedRequest request, final Session session)
            throws SpeechletException {
        log.info("onSessionEnded requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());
        // any cleanup logic goes here
    }

    /**
     *
     * @return
     */
    private SpeechletResponse getHelpResponse() {
        String speechText = "Please tell me movie name you would like to know review of. saying, tell me review of Titanic";
        return getSpeechletResponse(speechText, speechText, true);
    }

    /**
     *
     * @return
     */
    private SpeechletResponse getGoodByResponse() {
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText("Good bye!");
        return SpeechletResponse.newTellResponse(speech);
    }

    /**
     * Creates and returns a {@code SpeechletResponse} with a welcome message.
     *
     * @return SpeechletResponse spoken and visual welcome message
     */
    private SpeechletResponse getWelcomeResponse() {
        // Create the welcome message.
        String speechText =
                "Welcome to Movie Ratings App. Please tell me movie name you would like to know review of."
                        + " saying, tell me review of Titanic";
        String repromptText =
                "Please tell me movie name you would like to know review of by saying, tell me review of Titanic";

        return getSpeechletResponse(speechText, repromptText, true);
    }

    /**
     * Creates a {@code SpeechletResponse} for the intent and get the movie rating from the
     * IMDB.
     *
     * @param intent
     *            intent for the request
     * @return SpeechletResponse spoken and visual response for the intent
     */
    private SpeechletResponse getMovieRating(final Intent intent, final Session session) {
        // Get the slots from the intent.
        Map<String, Slot> slots = intent.getSlots();
        int success = 0;
        boolean isAskResponse = false;
        // String strMovieName = movieName.toString().trim();

        Slot movieNameSlot = slots.get(MOVIE_NAME_SLOT);
        String speechText = "Movie rating of ";


        if (movieNameSlot != null && movieNameSlot.getValue() != null) {
            String strMovieName = movieNameSlot.getValue();
            System.out.println("Movie name " + strMovieName);
            speechText += strMovieName + " is " + getMovieInfoFromOmdb(strMovieName);
            success = 1;
        } else {
            // Render an error since we don't know what the users movie name is.
            speechText = "I didn't get your movie name, please try again";
            isAskResponse = true;
        }
        String repromptText =
                "You can ask me movie rating by saying, what's movie rating of Titanic?";
        System.out.println("getMovieRating_Success=" + success);
        return getSpeechletResponse(speechText, repromptText, isAskResponse);
    }

    private SpeechletResponse getCastInformation(final Intent intent, final Session session) {
        
    }

    private String getMovieInfoFromOmdb(String movieName) {
        int success = 0;
        String rating = "not available.";
        OmdbApi omdb = new OmdbApi();
        try {
            OmdbVideoFull result = omdb.getInfo(new OmdbBuilder().setTitle(movieName).build());
            rating = result.getImdbRating() + " after " + result.getImdbVotes() + " votes.";
            success = 1;
        } catch (OMDBException e) {
            System.err.println("OMDB Exception for movie name " + movieName);
            e.printStackTrace();
        } finally {
            System.out.println("getMovieInfoFromOmdb_Success" + success);
        }

        return rating;
    }

    /**
     * Returns a Speechlet response for a speech and reprompt text.
     */
    private SpeechletResponse getSpeechletResponse(String speechText, String repromptText,
            boolean isAskResponse) {
        // Create the Simple card content.
        SimpleCard card = new SimpleCard();
        card.setTitle("BoxOffice");
        card.setContent(speechText);

        // Create the plain text output.
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(speechText);

        if (isAskResponse) {
            // Create reprompt
            PlainTextOutputSpeech repromptSpeech = new PlainTextOutputSpeech();
            repromptSpeech.setText(repromptText);
            Reprompt reprompt = new Reprompt();
            reprompt.setOutputSpeech(repromptSpeech);

            return SpeechletResponse.newAskResponse(speech, reprompt, card);

        } else {
            return SpeechletResponse.newTellResponse(speech, card);
        }
    }
}
