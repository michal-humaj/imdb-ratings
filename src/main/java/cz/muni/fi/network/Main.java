package cz.muni.fi.network;

import cz.muni.fi.datascrapper.DataTools;
import cz.muni.fi.datascrapper.model.Movie;
import cz.muni.fi.datascrapper.model.Person;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Created by MiHu on 11.11.2016.
 */
public class Main {

    public static void main(String[] args) throws Exception {

//        trainOnMovies();

        xorTraining();
    }

    private static void testTraining2() {
        ArrayList<Sample> samples = new ArrayList<>();
        for (int i = 0; i < 400; i++) {
            double x = new Random(i).nextInt((int) (2 * Math.PI * 1000)) / 1000d;
            samples.add(new Sample(new double[]{x / 7}, new double[] {(Math.sin(x) + 1) / 2}));
        }

        //                 Num Inputs,  Num Hidden,  Num Outputs,  Sigmoid steepness,
        MLP mlp = new MLP( 1,           2,           1,            1.0,
                //     Learning rate,  Weight init min,  Weight init max,  Print status frequency
                1,              -1,             1,                10   );

        mlp.training(samples);

        for(double x = 0.1; x < Math.PI * 2; x = x + 0.1) {
            System.out.printf("%.1f = %.4f%n", x, (Math.sin(x) + 1) / 2);
            System.out.printf("Výstup: %.4f%n", mlp.feedForward(new double[]{x / 7}, false)[0]);
            System.out.println();
        }

    }

    private static void xorTraining() {
        ArrayList<Sample> samples = new ArrayList<>();
        samples.add(new Sample(new double[]{1, 1}, new double[]{0}));
        samples.add(new Sample(new double[]{1, 0}, new double[]{1}));
        samples.add(new Sample(new double[]{0, 1}, new double[]{1}));
        samples.add(new Sample(new double[]{0, 0}, new double[]{0}));

        //                 Num Inputs,  Num Hidden,  Num Outputs,
        MLP mlp = new MLP( 2,           2,           1,
                //     Learning rate,  Weight init min,  Weight init max,  Print status frequency
                0.5,              0.2,             0.2,                10   );

        mlp.training(samples);
        System.out.println("----------------------------------------------------------------------");
        System.out.println("Vstup: [1,0] Výstup: " + mlp.feedForward(new double[]{1, 0}, true)[0]);
        System.out.println("Vstup: [1,1] Výstup: " + mlp.feedForward(new double[]{1, 1}, true)[0]);
        System.out.println("Vstup: [0,1] Výstup: " + mlp.feedForward(new double[]{0, 1}, true)[0]);
        System.out.println("Vstup: [0,0] Výstup: " + mlp.feedForward(new double[]{0, 0}, true)[0]);
    }

    private static void trainOnMovies() throws IOException{
        List<Person> actors = DataTools.getBaseActors();
        List<Movie> movies = DataTools.getMoviesFromJson()
                .stream().filter(m -> !m.shouldBeSkipped()).collect(Collectors.toList());

        for (Movie movie : movies) {
            for (String actor : movie.getActors()) {
                for (Person person : actors) {
                    if(person.getId().equals(actor)) {
                        person.count++;
                    }
                }
            }
        }

        Collections.sort(actors, (b,a) -> b.count - a.count);
        actors = actors.subList(0, 500);

        List<Sample> samples = new ArrayList<>();
//        Collections.shuffle(movies, new Random(55));
        Collections.sort(movies, (a,b) -> Float.compare(a.getRating(), b.getRating()));

        int trainingSize = 2000;

        for (Movie movie : movies) {
            if(--trainingSize == 0) {
                break;
            }
            double[] outputs = new double[] {movie.getRating() / 5 - 1};
            double[] inputs = new double[500];

            for (String actorId : movie.getActors()) {
                int i = actors.indexOf(new Person(actorId));
                if(i == -1) {
                    continue;
                }

                inputs[i] = 1;
            }

            samples.add(new Sample(inputs, outputs));
        }

        //                 Num Inputs,  Num Hidden,  Num Outputs,  Sigmoid steepness,
        MLP mlp = new MLP(500,           30,         1,            1,
                //     Learning rate,  Weight init min,  Weight init max,  Print status frequency
                0.1,              -0.1,             0.1,                10   );


        mlp.training(samples);

        for (int i = 0; i < 2000; i++) {
            double[] inputs = new double[500];

            for (String actorId : movies.get(i).getActors()) {
                int j = actors.indexOf(new Person(actorId));
                if(j == -1) {
                    continue;
                }
                inputs[j] = 1;
            }

            System.out.println(String.format("Movie : %s, rating: %.1f", movies.get(i).getName(), movies.get(i).getRating()));
            System.out.println(String.format("Predicted rating: %.1f", (mlp.feedForward(inputs, false)[0] + 1) * 5));

        }
    }
}
