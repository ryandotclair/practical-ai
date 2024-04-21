import Placeholder from 'Frontend/components/placeholder/Placeholder.js';
import {Suspense, useState} from 'react';
import {Outlet} from 'react-router-dom';
import TemporaryDrawer from "Frontend/views/drawer/Drawers";
import "@chatscope/chat-ui-kit-styles/dist/default/styles.min.css";
const navLinkClasses = ({ isActive }: any) => {
  return `block rounded-m p-s ${isActive ? 'bg-primary-10 text-primary' : 'text-body'}`;
};

export default function MainLayout() {
    const currentTitle = 'Vee';
    const [width, setWidth] = useState<number>(window.innerWidth);
    const isMobile = width <= 768;

  return (
      <div style={{"backgroundColor": "#ebf2f2" }}>
          <h2 className="text-l text-center m-0">
          </h2>
          <Suspense fallback={<Placeholder />}>
              <Outlet />
          </Suspense>
      </div>
  );
}
