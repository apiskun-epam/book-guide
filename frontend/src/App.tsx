import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Navigation from './components/Navigation';
import TopBar from './components/TopBar';
import CollectionPage from './pages/CollectionPage';
import ReadingListPage from './pages/ReadingListPage';
import ChatPage from './features/chat/ChatPage';
import { ToastProvider } from './components/ToastProvider';
import { CollectionRefreshProvider } from './components/CollectionRefresh';
import { AuthProvider, useAuth } from './auth/AuthProvider';
import AuthPage from './auth/AuthPage';
import './auth/AuthPage.css';

/** Renders nothing but the sign-in modal until a verified session exists. */
function AuthGate() {
  const { status } = useAuth();

  if (status === 'loading') {
    return <div className="auth-loading">Loading...</div>;
  }
  if (status === 'unauthenticated') {
    return <AuthPage />;
  }

  return (
    <CollectionRefreshProvider>
      <div className="app">
        <Navigation />
        <main className="app-main">
          <TopBar />
          <Routes>
            <Route path="/" element={<Navigate to="/collection" replace />} />
            <Route path="/collection" element={<CollectionPage />} />
            <Route path="/reading-list" element={<ReadingListPage />} />
            <Route path="/discover" element={<ChatPage />} />
          </Routes>
        </main>
      </div>
    </CollectionRefreshProvider>
  );
}

function App() {
  return (
    <ToastProvider>
      <Router>
        <AuthProvider>
          <AuthGate />
        </AuthProvider>
      </Router>
    </ToastProvider>
  );
}

export default App;
