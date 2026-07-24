import { Navigate } from "react-router-dom";
import { AppLayout } from "../layouts/AppLayout";
import { DashboardPage } from "../pages/DashboardPage";
import { GrammarPage } from "../pages/GrammarPage";
import { GrammarNotebookPage } from "../pages/GrammarNotebookPage";
import { GrammarPracticePage } from "../pages/GrammarPracticePage";
import { GrammarResultPage } from "../pages/GrammarResultPage";
import { LoginPage } from "../pages/LoginPage";
import { ProfilePage } from "../pages/ProfilePage";
import { RegisterPage } from "../pages/RegisterPage";
import { SpeakingConversationPage } from "../pages/SpeakingConversationPage";
import { SpeakingDailyPage } from "../pages/SpeakingDailyPage";
import { SpeakingFeedbackPage } from "../pages/SpeakingFeedbackPage";
import { SpeakingIeltsPage } from "../pages/SpeakingIeltsPage";
import { SpeakingIeltsTopicPage } from "../pages/SpeakingIeltsTopicPage";
import { SpeakingPage } from "../pages/SpeakingPage";
import { SpeakingScenarioDetailPage } from "../pages/SpeakingScenarioDetailPage";
import { VocabularyPage } from "../pages/VocabularyPage";
import { VocabularyPracticePage } from "../pages/VocabularyPracticePage";
import { VocabularyResultPage } from "../pages/VocabularyResultPage";
import { VocabularyWordbookPage } from "../pages/VocabularyWordbookPage";

export const appRoutes = [
  {
    path: "/",
    element: <AppLayout />,
    children: [
      { index: true, element: <DashboardPage /> },
      { path: "speaking", element: <SpeakingPage /> },
      { path: "speaking/daily", element: <SpeakingDailyPage /> },
      { path: "speaking/ielts", element: <SpeakingIeltsPage /> },
      { path: "speaking/ielts/:partId", element: <SpeakingIeltsTopicPage /> },
      { path: "speaking/:scenarioId/conversation", element: <SpeakingConversationPage /> },
      { path: "speaking/:scenarioId/feedback", element: <SpeakingFeedbackPage /> },
      { path: "speaking/:scenarioId", element: <SpeakingScenarioDetailPage /> },
      { path: "vocabulary", element: <VocabularyPage /> },
      { path: "vocabulary/practice/:level", element: <VocabularyPracticePage /> },
      { path: "vocabulary/result", element: <VocabularyResultPage /> },
      { path: "vocabulary/wordbook", element: <VocabularyWordbookPage /> },
      { path: "grammar", element: <GrammarPage /> },
      { path: "grammar/notebook", element: <GrammarNotebookPage /> },
      { path: "grammar/practice/:category", element: <GrammarPracticePage /> },
      { path: "grammar/result", element: <GrammarResultPage /> },
      { path: "profile", element: <ProfilePage /> },
      { path: "login", element: <LoginPage /> },
      { path: "register", element: <RegisterPage /> }
    ]
  },
  {
    // 未匹配到的地址统一回首页，避免出现空白页。
    path: "*",
    element: <Navigate to="/" replace />
  }
];
