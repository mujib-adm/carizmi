import { Component, ErrorInfo, ReactNode } from 'react';
import { Button, Result } from 'antd';

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
}

/**
 * Top-level error boundary that catches unhandled render errors and
 * shows a user-friendly fallback instead of a white screen.
 *
 * Must be a class component — React does not support error boundaries
 * with hooks as of React 18.
 */
export default class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError(): State {
    return { hasError: true };
  }

  componentDidCatch(error: Error, info: ErrorInfo): void {
    console.error('ErrorBoundary caught an error:', error, info);
  }

  private handleReload = (): void => {
    window.location.href = '/';
  };

  render(): ReactNode {
    if (this.state.hasError) {
      return (
        <Result
          status="error"
          title="Something went wrong"
          subTitle="An unexpected error occurred. Please try reloading the page."
          extra={
            <Button type="primary" onClick={this.handleReload}>
              Reload Application
            </Button>
          }
        />
      );
    }
    return this.props.children;
  }
}