import { Navigate } from "react-router-dom";
import { AppLayout } from "../layouts/AppLayout";
import { DashboardPage } from "../pages/DashboardPage";
import { GrammarPage } from "../pages/GrammarPage";
import { ProfilePage } from "../pages/ProfilePage";
import { SpeakingPage } from "../pages/SpeakingPage";
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
      { path: "vocabulary", element: <VocabularyPage /> },
      { path: "vocabulary/practice/:level", element: <VocabularyPracticePage /> },
      { path: "vocabulary/result", element: <VocabularyResultPage /> },
      { path: "vocabulary/wordbook", element: <VocabularyWordbookPage /> },
      { path: "grammar", element: <GrammarPage /> },
      { path: "profile", element: <ProfilePage /> }
    ]
  },
  {
    // 未匹配到的地址统一回首页，避免出现空白页。
    path: "*",
    element: <Navigate to="/" replace />
  }
];
