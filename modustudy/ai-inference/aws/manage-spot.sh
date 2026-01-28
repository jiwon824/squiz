#!/bin/bash
# Spot GPU 인스턴스 관리 스크립트
# 사용법: ./manage-spot.sh [start|stop|status|ssh]

set -e

# 설정 (본인 환경에 맞게 수정)
INSTANCE_ID="i-xxxxxxxxxxxxxxxxx"  # Spot 인스턴스 ID
KEY_FILE="~/.ssh/modustudy-gpu-key.pem"
REGION="ap-northeast-2"

# AWS CLI 프로필 (선택)
# export AWS_PROFILE=modustudy

get_instance_ip() {
    aws ec2 describe-instances \
        --instance-ids $INSTANCE_ID \
        --region $REGION \
        --query 'Reservations[0].Instances[0].PublicIpAddress' \
        --output text
}

get_instance_state() {
    aws ec2 describe-instances \
        --instance-ids $INSTANCE_ID \
        --region $REGION \
        --query 'Reservations[0].Instances[0].State.Name' \
        --output text
}

case "$1" in
    start)
        echo "Spot GPU 인스턴스 시작 중..."
        aws ec2 start-instances --instance-ids $INSTANCE_ID --region $REGION

        echo "시작 대기 중..."
        aws ec2 wait instance-running --instance-ids $INSTANCE_ID --region $REGION

        IP=$(get_instance_ip)
        echo "인스턴스 시작됨: $IP"
        echo ""
        echo "SSH 접속: ssh -i $KEY_FILE ubuntu@$IP"
        echo "API 주소: http://$IP:8000"
        ;;

    stop)
        echo "Spot GPU 인스턴스 중지 중..."
        aws ec2 stop-instances --instance-ids $INSTANCE_ID --region $REGION
        echo "중지 요청 완료"
        ;;

    status)
        STATE=$(get_instance_state)
        echo "상태: $STATE"

        if [ "$STATE" = "running" ]; then
            IP=$(get_instance_ip)
            echo "IP: $IP"
            echo "API: http://$IP:8000"

            # 헬스체크
            echo ""
            echo "헬스체크:"
            curl -s "http://$IP:8000/health" 2>/dev/null | python3 -m json.tool || echo "서버 응답 없음"
        fi
        ;;

    ssh)
        STATE=$(get_instance_state)
        if [ "$STATE" != "running" ]; then
            echo "인스턴스가 실행 중이 아닙니다: $STATE"
            exit 1
        fi

        IP=$(get_instance_ip)
        echo "SSH 접속: ubuntu@$IP"
        ssh -i $KEY_FILE ubuntu@$IP
        ;;

    ip)
        IP=$(get_instance_ip)
        echo $IP
        ;;

    *)
        echo "사용법: $0 {start|stop|status|ssh|ip}"
        echo ""
        echo "  start  - 인스턴스 시작"
        echo "  stop   - 인스턴스 중지 (비용 절감)"
        echo "  status - 상태 및 헬스체크"
        echo "  ssh    - SSH 접속"
        echo "  ip     - 현재 IP 출력"
        exit 1
        ;;
esac
