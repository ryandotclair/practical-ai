import MainLayout from 'Frontend/views/MainLayout.js';
import {createBrowserRouter, RouteObject} from 'react-router-dom';
import ChatView from "Frontend/views/chat/ChatView";

export const routes = [
  {
    element: <MainLayout />,
    handle: { title: 'Main' },
    children: [
      { path: '/', element: <ChatView />, handle: { title: 'chats' } },
    ],
  },
] as RouteObject[];

export default createBrowserRouter(routes);
