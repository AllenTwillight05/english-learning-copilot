package com.englishlearningcopilot.backend.config;

import com.englishlearningcopilot.backend.entity.SpeakingScenario;
import com.englishlearningcopilot.backend.repository.SpeakingScenarioRepository;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpeakingScenarioSeedConfig {

    @Bean
    CommandLineRunner seedSpeakingScenarios(SpeakingScenarioRepository scenarioRepository) {
        return args -> {
            List<SpeakingScenario> scenarios = List.of(
                    scenario(
                            "business-opening",
                            "Business Meeting",
                            "Practice opening a meeting, clarifying agenda items, and confirming delivery timelines.",
                            "B2",
                            "American business English",
                            "18 min",
                            "Train polite meeting openings and project update conversations.",
                            "blue",
                            "Open a meeting naturally, move the agenda forward, and confirm next steps.",
                            "agenda,clarify,timeline,follow up",
                            "You are a professional meeting partner. Keep the conversation realistic, concise, and focused on agenda, risks, timeline, and follow-up actions.",
                            "Good morning. Could you briefly introduce today's agenda?",
                            """
                                    Coach: Good morning. Could you briefly introduce today's agenda?
                                    Learner: Sure. Today I'd like to start with the project update, then discuss the delivery timeline.
                                    Coach: Great. How would you clarify a delayed deadline politely?
                                    Learner: I would say: Could we confirm whether Friday is still realistic for delivery?
                                    """,
                            6,
                            "Score fluency, grammar, vocabulary range, relevance, and business appropriateness."
                    ),
                    scenario(
                            "airport-checkin",
                            "Airport Check-in",
                            "Practice checking in, choosing seats, handling luggage, and asking for rescheduling help.",
                            "A2",
                            "Travel English",
                            "12 min",
                            "Train common airport check-in and rescheduling expressions.",
                            "gold",
                            "Complete basic check-in, seat selection, and rescheduling communication.",
                            "check in,boarding pass,aisle seat,reschedule",
                            "You are an airport check-in staff member. Ask practical travel questions and guide the learner through the check-in flow.",
                            "Hello. May I see your passport and booking reference?",
                            """
                                    Coach: Hello. May I see your passport and booking reference?
                                    Learner: Of course. Here is my passport, and this is my booking reference.
                                    Coach: Would you prefer a window seat or an aisle seat?
                                    Learner: I'd prefer an aisle seat if one is available.
                                    """,
                            5,
                            "Score clarity, task completion, grammar, vocabulary, and politeness."
                    ),
                    scenario(
                            "dinner-smalltalk",
                            "Dinner Small Talk",
                            "Practice casual openings, natural follow-up questions, and polite recommendations.",
                            "B1",
                            "Natural conversation",
                            "15 min",
                            "Train relaxed small talk and smooth follow-up questions.",
                            "mint",
                            "Start a light conversation and keep it going with natural questions.",
                            "small talk,recommend,by the way,sounds great",
                            "You are a friendly dinner companion. Keep the dialogue casual and encourage the learner to ask follow-up questions.",
                            "This restaurant is popular. Have you been here before?",
                            """
                                    Coach: This restaurant is popular. Have you been here before?
                                    Learner: No, it's my first time here. Do you have any recommendations?
                                    Coach: The pasta is great. What kind of food do you usually enjoy?
                                    Learner: I usually enjoy simple food with fresh ingredients.
                                    """,
                            6,
                            "Score fluency, interaction, grammar, vocabulary, and naturalness."
                    ),
                    scenario(
                            "clinic-visit",
                            "Clinic Visit",
                            "Practice describing symptoms, answering doctor questions, and confirming medicine instructions.",
                            "B1",
                            "Medical English",
                            "16 min",
                            "Train basic clinic communication and symptom descriptions.",
                            "rose",
                            "Describe symptoms clearly and understand simple medical instructions.",
                            "symptom,prescription,pharmacy,appointment",
                            "You are a clinic doctor. Ask careful but simple questions about symptoms, timeline, medicine, and allergies.",
                            "Hello. What brings you in today?",
                            """
                                    Coach: Hello. What brings you in today?
                                    Learner: I've had a sore throat and a headache for the past three days.
                                    Coach: Have you taken any medication for it so far?
                                    Learner: I took some painkillers yesterday, but the headache came back this morning.
                                    """,
                            6,
                            "Score clarity, symptom description, grammar, vocabulary, and relevance."
                    ),
                    scenario(
                            "G-01-airport",
                            "机场值机与改签",
                            "Practice checking in, choosing seats, handling luggage, and asking for rescheduling help.",
                            "A2",
                            "Travel English",
                            "12 min",
                            "训练值机、托运行李、选座和改签沟通。",
                            "gold",
                            "Complete basic check-in, seat selection, and rescheduling communication.",
                            "check in,boarding pass,aisle seat,reschedule",
                            "You are an airport check-in staff member. Ask practical travel questions and guide the learner through the check-in flow.",
                            "Hello. May I see your passport and booking reference?",
                            """
                                    Coach: Hello. May I see your passport and booking reference?
                                    Learner: Of course. Here is my passport, and this is my booking reference.
                                    Coach: Would you prefer a window seat or an aisle seat?
                                    Learner: I'd prefer an aisle seat if one is available.
                                    """,
                            5,
                            "Score clarity, task completion, grammar, vocabulary, and politeness."
                    ),
                    scenario(
                            "G-02-restaurant",
                            "餐厅点餐",
                            "Practice reservations, ordering, allergies, recommendations, and paying the bill.",
                            "A2",
                            "Daily English",
                            "12 min",
                            "练习订位、点餐、忌口说明和结账。",
                            "orange",
                            "Complete a full restaurant interaction from seating to payment.",
                            "reservation,recommend,allergy,bill",
                            "You are a restaurant server. Keep the exchange practical and polite.",
                            "Good evening. Do you have a reservation, or would you like a table for tonight?",
                            """
                                    Coach: Good evening. Do you have a reservation?
                                    Learner: No, we don't. Could we have a table for two?
                                    Coach: Of course. Do you have any food allergies?
                                    Learner: Yes, I can't eat peanuts.
                                    """,
                            5,
                            "Score clarity, ordering vocabulary, politeness, and task completion."
                    ),
                    scenario(
                            "G-03-hotel",
                            "酒店入住",
                            "Practice check-in, room confirmation, facility questions, and service problems.",
                            "A2",
                            "Travel English",
                            "13 min",
                            "练习入住、房型确认、设施咨询和投诉处理。",
                            "blue",
                            "Check in and handle common hotel service questions.",
                            "check in,reservation,amenities,reception",
                            "You are a hotel receptionist. Confirm details and help with common hotel requests.",
                            "Welcome to the hotel. May I have your booking name, please?",
                            """
                                    Coach: Welcome to the hotel. May I have your booking name?
                                    Learner: The booking is under Chen.
                                    Coach: You booked a double room for two nights. Is that correct?
                                    Learner: Yes, that's correct.
                                    """,
                            5,
                            "Score travel vocabulary, clarity, politeness, and problem solving."
                    ),
                    scenario(
                            "G-04-shopping",
                            "购物与退换货",
                            "Practice asking about size, price, discounts, returns, and exchanges.",
                            "A2",
                            "Shopping English",
                            "13 min",
                            "训练询价、试穿、折扣、退货和换货表达。",
                            "violet",
                            "Ask about product details and handle return or exchange requests.",
                            "size,discount,receipt,exchange",
                            "You are a shop assistant. Ask what the learner needs and help with return or exchange questions.",
                            "Welcome. What are you looking for today?",
                            """
                                    Coach: Welcome. What are you looking for today?
                                    Learner: I'm looking for a jacket in a larger size.
                                    Coach: Do you have the receipt for the exchange?
                                    Learner: Yes, here it is.
                                    """,
                            5,
                            "Score shopping vocabulary, politeness, and task completion."
                    ),
                    scenario(
                            "G-05-clinic",
                            "医院就诊",
                            "Practice registration, symptoms, medicine, allergies, and prescriptions.",
                            "B1",
                            "Medical English",
                            "16 min",
                            "练习挂号、描述症状、理解医嘱和取药。",
                            "rose",
                            "Describe symptoms clearly and understand simple medical instructions.",
                            "symptom,prescription,pharmacy,appointment",
                            "You are a clinic doctor. Ask careful but simple questions about symptoms, timeline, medicine, and allergies.",
                            "Hello. What brings you in today?",
                            """
                                    Coach: Hello. What brings you in today?
                                    Learner: I've had a sore throat and a headache for the past three days.
                                    Coach: Have you taken any medication for it so far?
                                    Learner: I took some painkillers yesterday.
                                    """,
                            6,
                            "Score symptom description, relevance, vocabulary, and clarity."
                    ),
                    scenario(
                            "G-06-job-interview",
                            "求职面试",
                            "Practice self-introduction, experience, strengths, challenges, and questions for the interviewer.",
                            "B1",
                            "Career English",
                            "18 min",
                            "练习自我介绍、经历说明、优势表达和反问。",
                            "mint",
                            "Answer common interview questions naturally and clearly.",
                            "experience,strength,challenge,role",
                            "You are an interviewer. Ask concise questions about experience, skills, motivation, and examples.",
                            "Thanks for coming today. Could you briefly introduce yourself?",
                            """
                                    Coach: Could you briefly introduce yourself?
                                    Learner: I'm a computer science student interested in product development.
                                    Coach: Could you tell me about one project you worked on?
                                    Learner: I built a learning app with a small team.
                                    """,
                            6,
                            "Score structure, relevance, vocabulary, fluency, and confidence."
                    ),
                    scenario(
                            "G-07-business-meeting",
                            "商务会议",
                            "Practice opening a meeting, clarifying agenda items, and confirming delivery timelines.",
                            "B2",
                            "Business English",
                            "18 min",
                            "训练开场、确认议程、表达意见和推进决策。",
                            "blue",
                            "Open a meeting naturally, move the agenda forward, and confirm next steps.",
                            "agenda,clarify,timeline,follow up",
                            "You are a professional meeting partner. Keep the conversation focused on agenda, risks, timeline, and follow-up actions.",
                            "Good morning. Could you briefly introduce today's agenda?",
                            """
                                    Coach: Good morning. Could you briefly introduce today's agenda?
                                    Learner: Today I'd like to start with the project update, then discuss the delivery timeline.
                                    Coach: How would you clarify a delayed deadline politely?
                                    Learner: Could we confirm whether Friday is still realistic?
                                    """,
                            6,
                            "Score fluency, grammar, vocabulary range, relevance, and business appropriateness."
                    ),
                    scenario(
                            "G-08-small-talk",
                            "日常寒暄",
                            "Practice casual openings, follow-up questions, and natural conversation endings.",
                            "B1",
                            "Natural conversation",
                            "15 min",
                            "训练破冰、接话、追问和自然结束话题。",
                            "mint",
                            "Start a light conversation and keep it going with natural questions.",
                            "small talk,by the way,sounds great,weekend",
                            "You are a friendly conversation partner. Encourage follow-up questions and natural responses.",
                            "Hi, good to see you. How has your week been?",
                            """
                                    Coach: Hi, good to see you. How has your week been?
                                    Learner: It has been busy, but pretty good.
                                    Coach: What did you do last weekend?
                                    Learner: I met some friends for dinner.
                                    """,
                            6,
                            "Score interaction, fluency, naturalness, grammar, and vocabulary."
                    ),
                    scenario(
                            "G-09-presentation",
                            "演讲展示",
                            "Practice opening, signposting, explaining data, and answering audience questions.",
                            "B2",
                            "Presentation English",
                            "18 min",
                            "练习开场、结构提示、数据说明和问答回应。",
                            "violet",
                            "Give a short structured presentation and answer follow-up questions.",
                            "overview,highlight,data,question",
                            "You are an audience member and presentation coach. Ask for structure, evidence, and concise answers.",
                            "Please start your presentation with a short overview of your topic.",
                            """
                                    Coach: Please start with a short overview.
                                    Learner: Today I will talk about our user research findings.
                                    Coach: What is the most important data point?
                                    Learner: The key point is that users want faster feedback.
                                    """,
                            6,
                            "Score structure, clarity, evidence, delivery, and response quality."
                    ),
                    scenario(
                            "G-10-phone-call",
                            "电话沟通",
                            "Practice explaining the reason for a call, confirming details, leaving messages, and rescheduling.",
                            "B1",
                            "Phone English",
                            "14 min",
                            "练习说明来意、确认信息、留言和改约。",
                            "gold",
                            "Speak clearly on the phone and confirm key information.",
                            "calling about,confirm,message,available",
                            "You are a receptionist or colleague on the phone. Ask the learner to clarify and confirm information.",
                            "Hello, this is Green Office. How can I help you?",
                            """
                                    Coach: Hello, this is Green Office. How can I help you?
                                    Learner: I'm calling about tomorrow's appointment.
                                    Coach: Could you confirm your name and phone number?
                                    Learner: Sure. My name is Li Wei.
                                    """,
                            5,
                            "Score clarity, phone etiquette, confirmation, and task completion."
                    ),
                    scenario(
                            "G-11-directions",
                            "问路与指路",
                            "Practice asking for directions, confirming routes, transport options, and travel time.",
                            "A2",
                            "Travel English",
                            "11 min",
                            "练习问路、确认路线、交通方式和到达时间。",
                            "orange",
                            "Understand and give simple route information.",
                            "turn left,cross,station,minutes",
                            "You are a helpful local person. Give clear route prompts and ask the learner to confirm directions.",
                            "Excuse me. Where would you like to go?",
                            """
                                    Coach: Excuse me. Where would you like to go?
                                    Learner: I'm trying to get to the train station.
                                    Coach: Go straight for two blocks, then turn left.
                                    Learner: So I go straight and turn left, right?
                                    """,
                            5,
                            "Score route vocabulary, clarity, listening confirmation, and politeness."
                    ),
                    scenario(
                            "G-12-renting-apartment",
                            "租房沟通",
                            "Practice apartment viewing, rent, deposit, lease terms, utilities, and maintenance.",
                            "B1",
                            "Housing English",
                            "17 min",
                            "练习看房、租金、押金、合同和维修沟通。",
                            "rose",
                            "Discuss rental conditions and contract details clearly.",
                            "rent,deposit,lease,maintenance",
                            "You are a landlord or property agent. Ask practical questions about budget, lease, and housing needs.",
                            "Thanks for coming to view the apartment. What kind of place are you looking for?",
                            """
                                    Coach: What kind of place are you looking for?
                                    Learner: I'm looking for a one-bedroom apartment near campus.
                                    Coach: What is your budget?
                                    Learner: Around eight hundred dollars per month.
                                    """,
                            6,
                            "Score housing vocabulary, clarity, negotiation, and task completion."
                    ),
                    scenario(
                            "IELTS-P1-practice",
                            "雅思口语 Part 1",
                            "Practice short IELTS Speaking Part 1 questions around a selected topic.",
                            "IELTS",
                            "Exam English",
                            "4-5 min",
                            "围绕所选话题进行 Part 1 短问短答训练。",
                            "blue",
                            "Answer directly, then add a reason or short example.",
                            "IELTS,Part 1,short answers",
                            "You are an IELTS examiner for Speaking Part 1. Ask brief natural questions and keep the pace exam-like.",
                            "Let's practise IELTS Speaking Part 1.",
                            """
                                    Examiner: Let's talk about your hometown.
                                    Candidate: Sure.
                                    Examiner: Do you like your hometown?
                                    Candidate: Yes, because it is convenient and friendly.
                                    """,
                            6,
                            "Score fluency, coherence, lexical resource, grammar range, and pronunciation."
                    ),
                    scenario(
                            "IELTS-P2-practice",
                            "雅思口语 Part 2",
                            "Practice IELTS Speaking Part 2 cue card long-turn answers.",
                            "IELTS",
                            "Exam English",
                            "3-4 min",
                            "围绕所选 cue card 训练个人陈述。",
                            "gold",
                            "Give a structured long-turn answer with details, feelings, and a conclusion.",
                            "IELTS,Part 2,cue card,long turn",
                            "You are an IELTS examiner for Speaking Part 2. Give cue-card follow-ups and help the learner extend the answer.",
                            "Let's practise IELTS Speaking Part 2.",
                            """
                                    Examiner: Describe a memorable journey.
                                    Candidate: I would like to talk about a trip to Hangzhou.
                                    Examiner: Please continue with more details about who you went with.
                                    Candidate: I went with two close friends.
                                    """,
                            5,
                            "Score fluency, coherence, lexical resource, grammar range, and pronunciation."
                    ),
                    scenario(
                            "IELTS-P3-practice",
                            "雅思口语 Part 3",
                            "Practice IELTS Speaking Part 3 abstract discussion questions.",
                            "IELTS",
                            "Exam English",
                            "4-5 min",
                            "围绕所选主题进行 Part 3 深入讨论。",
                            "mint",
                            "Develop opinions with reasons, examples, comparison, and prediction.",
                            "IELTS,Part 3,discussion,opinion",
                            "You are an IELTS examiner for Speaking Part 3. Ask abstract follow-up questions and invite explanation.",
                            "Let's practise IELTS Speaking Part 3.",
                            """
                                    Examiner: Let's discuss education and society.
                                    Candidate: Sure.
                                    Examiner: Why is education important for a country?
                                    Candidate: It helps people find better jobs and solve social problems.
                                    """,
                            6,
                            "Score fluency, coherence, lexical resource, grammar range, and pronunciation."
                    ),
                    scenario(
                            "IELTS-mock-test",
                            "雅思口语 Mock Test",
                            "Run a complete IELTS Speaking mock test across Part 1, Part 2, and Part 3.",
                            "IELTS",
                            "Exam English",
                            "11-14 min",
                            "按真实雅思口语考试节奏完成完整模拟。",
                            "violet",
                            "Train full-test stamina, structure, topic development, and exam timing.",
                            "IELTS,mock test,fluency,coherence",
                            "You are an IELTS examiner. Run a realistic mock test and move through Part 1, Part 2, and Part 3.",
                            "This is your IELTS Speaking mock test. Let's begin with Part 1.",
                            """
                                    Examiner: Do you work or are you a student?
                                    Candidate: I am a university student.
                                    Examiner: Describe a place you visited that you would recommend to others.
                                    Candidate: I would describe a city I visited last year.
                                    """,
                            12,
                            "Score fluency, coherence, lexical resource, grammar range, pronunciation, and exam task fit."
                    )
            );

            for (SpeakingScenario scenario : scenarios) {
                if (scenarioRepository.existsById(scenario.getId())) {
                    scenarioRepository.findById(scenario.getId())
                            .filter(existing -> existing.getSampleDialogue() == null || existing.getSampleDialogue().isBlank())
                            .ifPresent(existing -> {
                                existing.setSampleDialogue(scenario.getSampleDialogue());
                                scenarioRepository.save(existing);
                            });
                } else {
                    scenarioRepository.save(scenario);
                }
            }
        };
    }

    private SpeakingScenario scenario(
            String id,
            String title,
            String description,
            String difficulty,
            String accent,
            String duration,
            String summary,
            String tone,
            String goal,
            String keywords,
            String rolePrompt,
            String openingMessage,
            String sampleDialogue,
            int targetTurns,
            String scoringRubric
    ) {
        SpeakingScenario scenario = new SpeakingScenario();
        scenario.setId(id);
        scenario.setTitle(title);
        scenario.setDescription(description);
        scenario.setDifficulty(difficulty);
        scenario.setAccent(accent);
        scenario.setDuration(duration);
        scenario.setSummary(summary);
        scenario.setTone(tone);
        scenario.setGoal(goal);
        scenario.setKeywords(keywords);
        scenario.setRolePrompt(rolePrompt);
        scenario.setOpeningMessage(openingMessage);
        scenario.setSampleDialogue(sampleDialogue.strip());
        scenario.setTargetTurns(targetTurns);
        scenario.setScoringRubric(scoringRubric);
        scenario.setActive(true);
        return scenario;
    }
}
