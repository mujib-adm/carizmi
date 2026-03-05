import { SearchOutlined, UserOutlined } from '@ant-design/icons';
import { ConfigProvider, Empty, Select, Spin, Tag } from 'antd';
import { useEffect, useState } from 'react';
import { lookupMembers } from '../apiclient/memberApi';

import './member-lookup.css';

import type { SelectProps } from 'antd';
import './member-lookup.css';

interface MemberLookupProps extends SelectProps<any> {
  onSelectMember?: (member: any) => void;
  onError?: (error: any) => void;
}

export default function MemberLookup({
  value,
  onChange,
  onSelectMember,
  onError,
  ...rest
}: MemberLookupProps) {
  const [data, setData] = useState<any[]>([]);
  const [fetching, setFetching] = useState(false);

  const handleSearch = async (newValue: string) => {
    if (!newValue || newValue.length < 3) {
      setData([]);
      return;
    }
    setFetching(true);
    try {
      const result = await lookupMembers(newValue);
      if (result && result.responseData) {
        setData(result.responseData);
      } else {
        setData([]);
      }
    } catch (error) {
      if (onError) onError(error);
    } finally {
      setFetching(false);
    }
  };

  useEffect(() => {}, []);

  let timeout: ReturnType<typeof setTimeout> | null = null;
  const debouncedSearch = (val: string) => {
    if (timeout) clearTimeout(timeout);
    timeout = setTimeout(() => {
      handleSearch(val);
    }, 300);
  };

  const handleChange = (newValue: number, option: any) => {
    onChange?.(newValue);
    if (onSelectMember && option?.member) {
      onSelectMember(option.member);
    }
  };

  return (
    <ConfigProvider
      theme={{
        components: {
          Select: {
            controlHeight: 58,
            fontSize: 14,
            optionPadding: '12px 12px',
          },
        },
      }}
    >
      <div className="member-lookup-wrapper">
        <SearchOutlined className="member-lookup-prefix-icon" />
        <Select
          className="member-lookup-select"
          {...rest}
          showSearch
          value={value}
          placeholder={
            <div className="member-lookup-placeholder">Search Member (Name or ID)...</div>
          }
          defaultActiveFirstOption={false}
          filterOption={false}
          onSearch={debouncedSearch}
          onChange={handleChange}
          notFoundContent={fetching ? (<Spin size="small" />) : (<Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Type 3+ chars" />)}
          suffixIcon={<UserOutlined className="member-lookup-suffix-icon" />}
          options={(data || []).map((d: any) => ({
            value: d.memberID,
            label: `${d.firstName} ${d.lastName}`,
            member: d,
          }))}
          optionRender={(option) => {
            const d = option.data.member;
            return (
              <div className="member-option-container">
                <div className="member-option-info">
                  <span className="member-name">
                    {d.firstName} {d.lastName}
                  </span>
                  <span className="member-details">
                    ID: <span className="member-id-mono">{d.memberID}</span> • Phone: {d.phone}
                  </span>
                </div>
                {d.status !== 'Active'
                    ? (<Tag className="member-tag" color="error">{d.status}</Tag>)
                    : (<Tag className="member-tag" color="success">Active</Tag>)
                }
              </div>
            );
          }}
        />
      </div>
    </ConfigProvider>
  );
}