/**
    Copyright 2014-2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.

    Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with the License. A copy of the License is located at

        http://aws.amazon.com/apache2.0/

    or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package session;

import java.util.Map;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

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
import com.amazon.speech.ui.Card;
import com.amazon.speech.ui.Image;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SimpleCard;
import com.amazon.speech.ui.StandardCard;
import com.omertron.omdbapi.OMDBException;
import com.omertron.omdbapi.OmdbApi;
import com.omertron.omdbapi.model.OmdbVideoFull;
import com.omertron.omdbapi.tools.OmdbBuilder;

public class SessionSpeechlet implements Speechlet {

    private static final String MOVIE_NAME_SLOT = "MovieName";
    private static final String SKILL_TITLE = "BoxOffice";

    @Override
    public void onSessionStarted(final SessionStartedRequest request, final Session session)
            throws SpeechletException {
//        log.info("onSessionStarted requestId={}, sessionId={}", request.getRequestId(),
//                session.getSessionId());
        // any initialization logic goes here
    }

    @Override
    public SpeechletResponse onLaunch(final LaunchRequest request, final Session session)
            throws SpeechletException {
//        log.info("onLaunch requestId={}, sessionId={}", request.getRequestId(),
//                session.getSessionId());
        return getWelcomeResponse();
    }

    @Override
    public SpeechletResponse onIntent(final IntentRequest request, final Session session)
            throws SpeechletException {
//        log.info("onIntent requestId={}, sessionId={}", request.getRequestId(),
//                session.getSessionId());

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
        else if ("WhatsMovieCastIntent".equals(intentName)) {
            return getCastInformation(intent, session);
        }
        else if ("WhatsPlayingIntent".equals(intentName)) {
            return getMoviesInTheater(intent, session);
        }
        else {
            throw new SpeechletException("Invalid Intent");
        }
    }

    @Override
    public void onSessionEnded(final SessionEndedRequest request, final Session session)
            throws SpeechletException {
//        log.info("onSessionEnded requestId={}, sessionId={}", request.getRequestId(),
//                session.getSessionId());
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
        int success = 0;
        boolean isAskResponse = false;

        String speechText = "Movie rating of ";
        String movieName = getMovieName(intent);
        String posterUrl = null;

        if (!movieName.isEmpty()) {
            System.out.println("Movie name " + movieName);
            try {
                speechText += movieName + " is ";
                OmdbVideoFull omdbVideoFull = getMovieInfoFromOmdb(movieName);
                System.out.println("OmdbVideoFull "+ omdbVideoFull);

                posterUrl = omdbVideoFull.getPoster();
                String imdbRating = getImdbRating(omdbVideoFull);
                if ( !imdbRating.isEmpty()) {
                    speechText += imdbRating + " from IMDB";
                }

                String rottenTomatoRating = getRottenTomatoRating(omdbVideoFull);
                if ( !rottenTomatoRating.isEmpty()) {
                    speechText +=  rottenTomatoRating + " from Rotten Tomato.";
                }
                success = 1;
            } catch (OMDBException e) {
                speechText += "not available";
                e.printStackTrace();
            }
        } else {
            // Render an error since we don't know what the users movie name is.
            speechText = "I didn't get your movie name, please try again";
            isAskResponse = true;
        }
        String repromptText =
                "You can ask me movie rating by saying, what's movie rating of Titanic?";
        System.out.println("getMovieRating_Success=" + success);
        return getSpeechletResponse(speechText, repromptText, posterUrl, isAskResponse);
    }

    private String getImdbRating(OmdbVideoFull movieInformation) {
        String imdbRating = "";
        int success = 0;
        try {
            imdbRating = movieInformation.getImdbRating();
            success = 1;
        }
        catch (Exception e) {
            System.out.println("Failed to get IMDB rating " + e);
        }
        finally {
            System.out.println("getImdbRating_Success="+ success);
        }
        return imdbRating;
    }

    private String getRottenTomatoRating(OmdbVideoFull movieInformation) {
        String rottenTomatoRating = "";
        int success = 0;
        try {
            rottenTomatoRating = movieInformation.getTomatoRating();
            System.out.println("Rotten tomato rating = " + rottenTomatoRating);
            success = 1;
        }
        catch (Exception e) {
            System.out.println("Failed to get RottenTomato"+ e);
        }
        finally {
            System.out.println("getRottenTomatoRating_Success=" + success);
        }
        return rottenTomatoRating;
    }

    private SpeechletResponse getCastInformation(final Intent intent, final Session session) {
        String speechText = "";
        String repromptText = "";
        boolean isAskResponse = false;
        int success = 0;
        String movieName = getMovieName(intent);
        if (!movieName.isEmpty()) {
            try {
                speechText = getMovieInfoFromOmdb(movieName).getActors();
                success = 1;
            } catch (OMDBException e) {
                speechText = "Actors list is not available.";
            }
        }
        System.out.println("getCastInformation_Success=" + success);
        return getSpeechletResponse(speechText, repromptText, isAskResponse);

    }

    private OmdbVideoFull getMovieInfoFromOmdb(String movieName) throws OMDBException {
        int success = 0;
        OmdbApi omdb = new OmdbApi();
        OmdbVideoFull result;
        try {
            result = omdb.getInfo(new OmdbBuilder().setTitle(movieName).build());
            success = 1;
        } catch (OMDBException e) {
            System.err.println("OMDB Exception for movie name " + movieName);
            throw e;
        } finally {
            System.out.println("getMovieInfoFromOmdb_Success=" + success);
        }
        return result;
    }

    private String getMovieName(final Intent intent) {
        // Get the slots from the intent.
        Map<String, Slot> slots = intent.getSlots();
        int success = 0;
        String movieName = "";
        Slot movieNameSlot = slots.get(MOVIE_NAME_SLOT);
        if (movieNameSlot != null && movieNameSlot.getValue() != null) {
            movieName = movieNameSlot.getValue();
            success = 1;
        }
        System.out.println("getMovieName_Success=" + success);
        return movieName;
    }

    private SpeechletResponse getMoviesInTheater(final Intent intent, final Session session) {
        String speechText = "Developer is still working on this feature. Coming soon";
        String repromptText = "";
        boolean isAskResponse = false;
        int success = 1;

        System.out.println("getMoviesInTheater_Success=" + success);
        return getSpeechletResponse(speechText, repromptText, isAskResponse);

    }

    private Image createPoster(String posterUrl) {
        Image poster = new Image();
        poster.setSmallImageUrl(posterUrl.replace("http://", "https://"));
        return poster;
    }

    private Card createCardWithoutPoster(String speechText) {
        SimpleCard card = new SimpleCard();
        card.setTitle(SKILL_TITLE);
        card.setContent(speechText);
        return card;
    }

    private Card createCardWithPoster(String speechText, String posterUrl) {
        StandardCard card = new StandardCard();
        card.setTitle(SKILL_TITLE);
        card.setText(speechText);
        Image poster = createPoster(posterUrl);
        if (poster != null) {
            card.setImage(poster);
        }
        return card;
    }

    private Card createCard(String speechText, String posterUrl) {
        if (posterUrl != null && !posterUrl.isEmpty()) {
            return createCardWithPoster(speechText, posterUrl);
        } else {
            return createCardWithoutPoster(speechText);
        }
    }

    /**
     * Returns a Speechlet response for a speech and reprompt text.
     */
    private SpeechletResponse getSpeechletResponse(String speechText, String repromptText, String posterUrl,
            boolean isAskResponse) {

        // Create the plain text output.
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(speechText);

        if (isAskResponse) {
            // Create reprompt
            PlainTextOutputSpeech repromptSpeech = new PlainTextOutputSpeech();
            repromptSpeech.setText(repromptText);
            Reprompt reprompt = new Reprompt();
            reprompt.setOutputSpeech(repromptSpeech);

            return SpeechletResponse.newAskResponse(speech, reprompt, createCard(speechText, posterUrl));

        } else {
            return SpeechletResponse.newTellResponse(speech, createCard(speechText, posterUrl));
        }
    }

    private SpeechletResponse getSpeechletResponse(String speechText, String repromptText,
            boolean isAskResponse) {
        return getSpeechletResponse(speechText, repromptText, null, isAskResponse);

    }
}
