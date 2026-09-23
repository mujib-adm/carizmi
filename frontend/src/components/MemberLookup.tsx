import { SearchOutlined, UserOutlined } from '@ant-design/icons';
import { ConfigProvider, Empty, Select, Spin, Tag } from 'antd';
import { useEffect, useRef, useState } from 'react';
import { membersApi } from '../api/generated/members/members';
import type { MemberLookupDto } from '../api/generated/types/index';

import type { SelectProps } from 'antd';
import '../styles/components/MemberLookup.css';

interface MemberLookupProps extends SelectProps<number> {
  onSelectMember?: (member: MemberLookupDto | null) => void;
  onError?: (error: any) => void;
  compact?: boolean;
}

export default function MemberLookup({
  value,
  onChange,
  onSelectMember,
  onError,
  compact = false,
  className,
  style,
  ...rest
}: MemberLookupProps) {
  const [data, setData] = useState<MemberLookupDto[]>([]);
  const [fetching, setFetching] = useState(false);

  const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const handleSearch = async (newValue: string) => {
    if (!newValue || newValue.length < 3) {
      setData([]);
      return;
    }
    setFetching(true);
    try {
      const result = await membersApi.lookupMembers({ query: newValue });
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

  useEffect(() => {
    return () => {
      if (timeoutRef.current) clearTimeout(timeoutRef.current);
    };
  }, []);

  const debouncedSearch = (val: string) => {
    if (timeoutRef.current) clearTimeout(timeoutRef.current);
    timeoutRef.current = setTimeout(() => {
      handleSearch(val);
    }, 300);
  };

  const handleChange = (newValue: number, option: any) => {
    onChange?.(newValue);
    if (onSelectMember) {
      onSelectMember(option?.member ?? null);
    }
  };

  return (
    <ConfigProvider
      theme={{
        components: {
          Select: {
            controlHeight: compact ? 32 : 58,
            fontSize: compact ? 13 : 14,
            optionPadding: compact ? '6px 12px' : '12px 12px',
          },
        },
      }}
    >
      <div
        className={`member-lookup-wrapper ${compact ? 'member-lookup-compact' : ''}`}
        style={compact ? { width: 'auto', display: 'inline-block' } : undefined}
      >
        <SearchOutlined
          className={`member-lookup-prefix-icon ${compact ? 'member-lookup-prefix-icon-compact' : ''}`}
        />
        <Select
          className={`member-lookup-select ${compact ? 'member-lookup-select-compact' : ''} ${className || ''}`}
          style={{ width: compact ? 220 : '100%', ...style }}
          popupMatchSelectWidth={compact ? false : undefined}
          styles={compact ? { popup: { root: { minWidth: 220 } } } : undefined}
          {...rest}
          showSearch
          value={value}
          placeholder={
            <div className="member-lookup-placeholder">Search Member...</div>
          }
          defaultActiveFirstOption={false}
          filterOption={false}
          onSearch={debouncedSearch}
          onChange={handleChange}
          notFoundContent={
            fetching ? (
              <Spin size="small" />
            ) : (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Type 3+ chars" />
            )
          }
          suffixIcon={<UserOutlined className="member-lookup-suffix-icon" />}
          options={(data || []).map((d: MemberLookupDto) => ({
            value: d.memberID,
            label: `${d.firstName} ${d.lastName}`,
            member: d,
          }))}
          optionRender={(option) => {
            const d = option.data.member;
            if (compact) {
              return (
                <div className="member-option-container member-option-container-compact">
                  <div className="member-option-info">
                    <span className="member-name">
                      {d.firstName} {d.lastName}
                    </span>
                    {d.phone && (
                      <span className="member-details">
                        Phone: {d.phone}
                      </span>
                    )}
                  </div>
                </div>
              );
            }
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
                {d.status == '01' ? (
                  <Tag className="member-tag" color="success">
                    Active
                  </Tag>
                ) : (
                  <Tag className="member-tag" color="error">
                    {d.status}
                  </Tag>
                )}
              </div>
            );
          }}
        />
      </div>
    </ConfigProvider>
  );
}