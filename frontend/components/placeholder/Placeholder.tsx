import { ProgressBar } from '@hilla/react-components/ProgressBar.js';

export default function Placeholder() {
  return <ProgressBar indeterminate={true} className="m-0" value={0.5} theme={"success"} />;
}
