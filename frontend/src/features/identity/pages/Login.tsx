import { useForm } from 'react-hook-form';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../../hooks/useAuth';
import '../../../styles/pages/AuthForm.css';

import apiClient from '../../../api/client/ApiClient';
import { FormField } from '../../../components/FormField';
import { MessageBanner } from '../../../components/MessageBanner';
import { AUTH_LOGIN } from '../../../api/constants/customEndpoints';
import { LoginRequestDto } from '../../../api/constants/customTypes';
import { GlobalResponse, MessageType } from '../../../api/generated/types/index';
import { useApiMessages } from '../../../hooks/useApiMessages';

export default function Login() {
  const navigate = useNavigate();
  const { login } = useAuth();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginRequestDto>();
  const { globalMessages, handleResponse, handleError, resetMessages } =
    useApiMessages<LoginRequestDto>();

  const onSubmit = async (formValues: LoginRequestDto) => {
    // clear old messages from MessageBanner before starting new call
    resetMessages();
    try {
      const response = await apiClient.post<GlobalResponse>(AUTH_LOGIN, formValues);
      const responseBody = response.data;

      if (responseBody?.responseData?.role) {
        login(responseBody.responseData.role, responseBody.responseData.firstName || '');
        navigate('/dashboard');
      } else if (responseBody.globalMessages?.length > 0) {
        handleResponse(responseBody);
      } else {
        handleResponse({
          statusCode: 500,
          statusDesc: 'Error',
          globalMessages: [
            { type: MessageType.ERROR, message: 'Login failed. Invalid server response.' },
          ],
          fieldMessages: [],
        });
      }
    } catch (error: any) {
      handleError(error);
    }
  };

  return (
    <div className="modern-login-container">
      <div className="login-glass-card">
        <form onSubmit={handleSubmit(onSubmit)}>
          <h2>LOGIN</h2>

          <div className="modern-field-label">
            USERNAME <span className="required">*</span>
          </div>
          <FormField
            type="text"
            placeholder="Enter username..."
            registerProps={register('username', { required: 'Username is required' })}
            error={errors.username}
          />

          <div style={{ marginTop: '20px' }}>
            <div className="modern-field-label">
              PASSWORD <span className="required">*</span>
            </div>
            <FormField
              type="password"
              placeholder="Enter password..."
              registerProps={register('password', { required: 'Password is required' })}
              error={errors.password}
            />
          </div>

          {globalMessages && <MessageBanner messages={globalMessages} />}

          <button className="global_btn" type="submit">
            LOGIN
          </button>
        </form>
      </div>
    </div>
  );
}