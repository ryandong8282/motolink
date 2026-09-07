import 'package:flutter_test/flutter_test.dart';
import 'package:motolink_mobile/src/app.dart';

void main() {
  testWidgets('renders the MotoLink MVP home screen', (tester) async {
    await tester.pumpWidget(const MotoLinkApp());

    expect(find.text('MotoLink'), findsOneWidget);
    expect(find.text('附近车友'), findsOneWidget);
    expect(find.text('进入车队对讲'), findsOneWidget);
  });
}
