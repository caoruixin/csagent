#  The current salesforce part spec

## 问题1：create_case_controlled per-UC required_fields 终稿  + Queue 路由表（UC-H / UC-J / UC-K）

1、聊天前，自动创建的case，record_type是 Customer Service
2、Case字段
First Name（必填）、Last Name（必填）、Email（必填）、Topic Subject（必填）、Ad ID Number（选填）、Description（必填）
3、Topic Subject 可选值：
    序号	选项值
    1	Account Support
    2	Ad Support
    3	Delete My Account or Data
    4	Delivery
    5	Payments
    6	Pro Contract
    7	Account Manager Support
    8	Ratings Reviews
    9	Replies or Messaging
    10	Report a Safety Issue
    11	Technical Support
4、客服离线时的表单创建case后会立即转到队列：CS_Cases_New 
5、客服在线时的表单会分给接线客服。聊天队列：CS_NEW_chat，客服主动认领会话。
6、在线客服可最大并发处理 2个会话


## 问题2：Salesforce 组织配置确认（Enhanced Chat / Omni-Channel / Knowledge API 版本与可用能力）

Enhanced Chat 版本：Web (v1)，
    提供了预置的Queue：New Chat，可以用来判断客服在线状态。
    预定义表单功能，可以用来创建case、contact、account。
    自研bot最多50次会话，超过50次会话，需要转人工。
Omni-Channel 标准通道
Knowledge API 现在暂无开发api能力，无法使用

## 问题3：Off-hours 策略（is_business_hours 判定 + 离线时段 Bot 行为边界）

is_business_hours 判定：
    1、判断当前Channel名称Messaging的queue：New Chat，是否有agent在线
    2、如果在线，则认为是业务时间
    3、如果不在线，则认为是离线时间
离线时段 Bot 行为边界：处理不了的问题，需要转人工时 友好说明目前客服离线，工单转到CS_Cases_New队列，待客服接待case处理。


## 问题4：Salesforce 自定义对象/字段命名最终方案（Bot_Session__c / Bot_Event__c / Bot_Context__c 是否采用，或使用既有对象）
     
    目前自定义表只有这一个：
    Chat_Message_Log__c：用来记录bot与客户之间的会话消息，包括消息类型、消息时间、消息内容、消息状态等。
        Name：Text(80)		True	
        CreatedById	Lookup(User)	True	
        Created_Date__c	Date/Time		False	
        Direction__c	Picklist（Inbound、Outbound）	False	
        LastModifiedById	Lookup(User)	True	
        Message_Text__c	Long Text Area(32768)	False	
        OwnerId	Lookup(User,Group)	True	
        Processed__c	Checkbox	False	
        Response_Text__c	Long Text Area(32768)	False	
        Sender_Type__c	Picklist（Agent、Customer、Bot）	False	
        Session_Id__c	Text(255)
    
    bot输出json内容示例：
    {
        "replyText": "Hello, how can I help you today?",
        "intent": "TRANSFER_TO_HUMAN",
        "shouldEndChat": false,
        "additionalData": {
            "customerName": "John Doe"
        }
    }


## 补充1 预填写表单与case创建
- 用户开始chat前，会先填写一个表单
  - 1，数据集对应的是老版本表单，其中的字段和当前production environment的新版本有差别，且必填字段要求不一样，表单中有 "Subject, Reason, Description fields". 
  - 2，production environment对应的是新版本："first name, last name, email, Topic Subject ,Description" fields 这些字段都为必填 "Ad ID Number" 为optional
- 表单提交后会先创建case，再进行case流转
  - 工作时间段，这个case会流转给可以处理from chat 的客服，用户提交的信息客服在自己的console中看到，相当于user已经通过 form 提交了 一轮 信息，当客服收到 chat case 时，对于用户的问题已经有了大致的了解，因此也解释了为什么每一个chat case 在session开始时， 1st turn 留言 基本都是客服的留言 
  - 非工作时间，这个case会流转到另外一个queue/skillgroup，由客服离线处理
- 预填写表单中的信息，可以将其视为 turn0