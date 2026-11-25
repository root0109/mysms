package com.mysms.demoseed.seed;

import com.mysms.demoseed.client.Filter;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import tools.jackson.databind.JsonNode;

/**
 * Invented people for the demo school: given and family names common in India combined at random from a fixed seed
 * (so no real person is described), mail addresses on the reserved {@code .example} domain, and the people record
 * each of them gets. A person is found again by that address.
 */
public final class DemoPeople {

    /** The domain every demo address is on; reserved, so no mail ever leaves for a real mailbox. */
    public static final String DOMAIN = "sunrisevalley.example";

    private static final List<String> MALE = List.of("Aarav", "Vivaan", "Aditya", "Vihaan", "Arjun", "Sai", "Reyansh",
            "Ayaan", "Krishna", "Ishaan", "Shaurya", "Atharv", "Advik", "Pranav", "Kabir", "Rohan", "Dhruv", "Kartik",
            "Manav", "Yash", "Rahul", "Nikhil", "Siddharth", "Varun", "Harsh", "Tejas", "Om", "Lakshya", "Parth",
            "Rudra", "Anand", "Suresh", "Rajesh", "Vikram", "Sanjay", "Mahesh", "Prakash", "Deepak", "Ashok",
            "Naveen");
    private static final List<String> FEMALE = List.of("Saanvi", "Ananya", "Aadhya", "Diya", "Pari", "Anika", "Navya",
            "Myra", "Sara", "Ira", "Kiara", "Riya", "Ishita", "Meera", "Aditi", "Kavya", "Tanvi", "Nisha", "Pooja",
            "Sneha", "Shreya", "Avni", "Prisha", "Khushi", "Jhanvi", "Lavanya", "Radhika", "Sunita", "Anjali",
            "Kavita", "Rekha", "Lakshmi", "Priya", "Neha", "Swati", "Divya", "Geeta", "Shalini", "Usha", "Vandana");
    private static final List<String> FAMILY = List.of("Sharma", "Verma", "Iyer", "Nair", "Reddy", "Patel", "Gupta",
            "Mehta", "Joshi", "Kulkarni", "Deshpande", "Menon", "Pillai", "Rao", "Naidu", "Chatterjee", "Banerjee",
            "Mukherjee", "Das", "Bose", "Singh", "Chauhan", "Yadav", "Mishra", "Pandey", "Tiwari", "Saxena",
            "Agarwal", "Bhat", "Kamath", "Shetty", "Hegde", "Gowda", "Krishnan", "Subramanian", "Fernandes",
            "D'Souza", "Kapoor", "Malhotra", "Khanna");

    private DemoPeople() {
    }

    /** A person's invented name and gender. */
    public record Name(String given, String family, String gender) {

        public String display() {
            return given + " " + family;
        }

        /** {@code given.family.<tag>@sunrisevalley.example}; the tag keeps addresses apart. */
        public String email(String tag) {
            return (given + "." + family + "." + tag).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9.]", "")
                    + "@" + DOMAIN;
        }
    }

    public static Name name(Random random) {
        boolean female = random.nextBoolean();
        List<String> given = female ? FEMALE : MALE;
        return new Name(given.get(random.nextInt(given.size())), FAMILY.get(random.nextInt(FAMILY.size())),
                female ? "female" : "male");
    }

    /** A name of the given gender and family. */
    public static Name name(Random random, String family, String gender) {
        List<String> given = "female".equals(gender) ? FEMALE : MALE;
        return new Name(given.get(random.nextInt(given.size())), family, gender);
    }

    /** The people record of a demo person, found by its address or created. */
    public static JsonNode person(SeedContext context, Name name, LocalDate birth, String email) {
        JsonNode body = context.object().put("firstName", name.given()).put("lastName", name.family())
                .put("displayName", name.display()).put("gender", name.gender())
                .put("dateOfBirth", birth.toString()).put("primaryEmail", email);
        return context.ensure("people", "person", "/v1/people/persons", Filter.eq("primaryEmail", email), body);
    }
}
